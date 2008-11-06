package org.swami.om2.neorepo.sparql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.levering.ryan.sparql.common.QueryException;
import name.levering.ryan.sparql.model.FilterConstraint;
import name.levering.ryan.sparql.model.GroupConstraint;
import name.levering.ryan.sparql.model.OptionalConstraint;
import name.levering.ryan.sparql.model.TripleConstraint;
import name.levering.ryan.sparql.model.logic.ExpressionLogic;
import name.levering.ryan.sparql.parser.model.ASTAndNode;
import name.levering.ryan.sparql.parser.model.ASTEqualsNode;
import name.levering.ryan.sparql.parser.model.ASTGreaterThanEqualsNode;
import name.levering.ryan.sparql.parser.model.ASTGreaterThanNode;
import name.levering.ryan.sparql.parser.model.ASTLessThanEqualsNode;
import name.levering.ryan.sparql.parser.model.ASTLessThanNode;
import name.levering.ryan.sparql.parser.model.ASTLiteral;
import name.levering.ryan.sparql.parser.model.ASTOrNode;
import name.levering.ryan.sparql.parser.model.ASTQName;
import name.levering.ryan.sparql.parser.model.ASTQuotedIRIref;
import name.levering.ryan.sparql.parser.model.ASTRegexFuncNode;
import name.levering.ryan.sparql.parser.model.ASTVar;
import name.levering.ryan.sparql.parser.model.BinaryExpressionNode;
import name.levering.ryan.sparql.parser.model.URINode;

import org.neo4j.api.core.RelationshipType;
import org.neo4j.util.matching.PatternGroup;
import org.neo4j.util.matching.PatternNode;
import org.neo4j.util.matching.PatternRelationship;
import org.neo4j.util.matching.filter.CompareExpression;
import org.neo4j.util.matching.filter.FilterBinaryNode;
import org.neo4j.util.matching.filter.FilterExpression;
import org.neo4j.util.matching.filter.RegexPattern;
import org.openrdf.model.URI;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.swami.om2.neorepo.sparql.MetaModelProxy.OwlPropertyType;
import org.swami.om2.neorepo.sparql.NeoVariable.VariableType;

public class QueryGraph
{
	private Map<ExpressionLogic, PatternNode> graph =
		new HashMap<ExpressionLogic, PatternNode>();
	private List<NeoVariable> variableList;
	private List<QueryGraph> optionalGraphs = new LinkedList<QueryGraph>();
	private Map<String, PatternNode> nodeTypes =
		new HashMap<String, PatternNode>();
	private Set<PatternNode> possibleStartNodes = new HashSet<PatternNode>();
	protected MetaModelProxy metaModel;
	private Map<ExpressionLogic, String> classMapping =
		new HashMap<ExpressionLogic, String>();

	QueryGraph( MetaModelProxy metaModel, List<NeoVariable> variableList )
	{
		this.variableList = variableList;
		this.metaModel = metaModel;
	}

	PatternNode getStartNode()
	{
		int lowestCount = Integer.MAX_VALUE;
		PatternNode startNode = null;
		
		for ( PatternNode node : this.possibleStartNodes )
		{
			int count = this.metaModel.getCount( node.getLabel() );
			
			if ( count < lowestCount )	
			{
				lowestCount = count;
				startNode = node;
			}
		}
		
		return startNode;
	}
	
	PatternNode findTypeNode()
	{
		return this.possibleStartNodes.isEmpty() ? null :
			this.possibleStartNodes.iterator().next();
	}

	Collection<PatternNode> getOptionalGraphs()
	{
		Collection<PatternNode> optionalStartNodes =
			new ArrayList<PatternNode>();
		
		for ( QueryGraph optionalGraph : this.optionalGraphs )
		{
			optionalStartNodes.add( this.getOverLappingNode( optionalGraph ) );
		}
		return optionalStartNodes;
	}

	private PatternNode getOverLappingNode( QueryGraph optionalGraph )
	{
		for ( PatternNode node : optionalGraph.graph.values() )
		{
			for ( PatternNode mainNode : this.graph.values() )
			{
				if ( node.getLabel().equals( mainNode.getLabel() ) )
				{
					return node;
				}
			}
		}
		
		throw new QueryException(
			"Optional graphs must be connected to the main statements" );
	}

	void build( GroupConstraint groupConstraint )
	{
		this.build( groupConstraint, false );
	}

	void build( GroupConstraint groupConstraint, boolean optional )
	{
	    PatternGroup group = new PatternGroup();
		Set<TripleConstraint> typeConstraints =
			new HashSet<TripleConstraint>();
		Set<TripleConstraint> normalConstraints =
			new HashSet<TripleConstraint>();
		Set<FilterConstraint> filterConstraints =
		    new HashSet<FilterConstraint>();
	
		for ( Object constraint : groupConstraint.getConstraints() )
		{
			if ( constraint instanceof TripleConstraint )
			{
				if ( this.metaModel.isTypeProperty(
					this.toUri( ( ( TripleConstraint ) constraint ).
						getPredicateExpression() ) ) )
				{
					typeConstraints.add( ( TripleConstraint ) constraint );
				}
				else
				{
					normalConstraints.add( ( TripleConstraint ) constraint );
				}
			}
			else if ( constraint instanceof OptionalConstraint )
			{
				QueryGraph optionalGraph =
					new QueryGraph( this.metaModel, this.variableList );
				optionalGraph.build( ( ( OptionalConstraint )
					constraint ).getConstraint(), true );
				this.optionalGraphs.add( optionalGraph );
			}
			else if ( constraint instanceof FilterConstraint )
			{
			    filterConstraints.add( ( FilterConstraint ) constraint );
			}
			else
			{
				throw new QueryException(
					"Operation not supported with NeoRdfSource." );
			}
		}
	
		// Must add types before the other constraints.
		this.addTypes( typeConstraints, group, optional );
		this.addConstraints( normalConstraints, group, optional );
		this.addFilters( filterConstraints, group, optional );
	}
	
	private void addTypes( Set<TripleConstraint> constraints,
	    PatternGroup group, boolean optional )
	{
		for ( TripleConstraint constraint : constraints )
		{
			this.addTypeToPattern( constraint, group, optional );
		}
	}

	private void addTypeToPattern(
		TripleConstraint constraint, PatternGroup group, boolean optional )
	{
		this.assertConstraint( constraint );
		PatternNode subjectNode = this.getOrCreatePatternNode(
			constraint.getSubjectExpression(), group );
		PatternNode objectNode;
		if ( constraint.getObjectExpression() instanceof ASTVar )
		{
			objectNode = this.getOrCreatePatternNode(
				constraint.getObjectExpression(), group, true );
			this.addVariable( ( ASTVar ) constraint.getObjectExpression(), 
				NeoVariable.VariableType.LITERAL, objectNode,
				this.metaModel.getNodeTypeNameKey() );
			this.possibleStartNodes.add( objectNode );
		}
		else
		{
			objectNode = this.getOrCreatePatternNode(
				constraint.getObjectExpression(), group, true,
				ON_CREATED_TYPE );
			String objectUri = this.toUri( constraint.getObjectExpression() );
			this.classMapping.put( constraint.getSubjectExpression(),
				objectUri );
			this.nodeTypes.put( objectUri, objectNode );
		}
		subjectNode.createRelationshipTo( objectNode,
			this.metaModel.getTypeRelationship(), optional );
	}
	
	private void addConstraints( Set<TripleConstraint> constraints,
	    PatternGroup group, boolean optional )
	{
		for ( TripleConstraint constraint : constraints )
		{
			this.addToPattern( constraint, group, optional );
		}
	}
	
	private void addFilters( Set<FilterConstraint> filterConstraints,
	    PatternGroup group, boolean optional )
	{
	    for ( FilterConstraint filterConstrains : filterConstraints )
	    {
	        this.addFilter( filterConstrains, group, optional );
	    }
	}

	protected void addToPattern( TripleConstraint constraint,
	    PatternGroup group, boolean optional )
	{
		this.assertConstraint( constraint );
		this.addOwlProperty( constraint.getSubjectExpression(),
			constraint.getPredicateExpression(),
			constraint.getObjectExpression(), group, optional );
	}
	
	protected void addFilter( FilterConstraint filterConstraint,
	    PatternGroup group, boolean optional )
	{
	    group.addFilter(
	        toFilterExpression( filterConstraint.getExpression() ) );
	}
	
	private FilterExpression toFilterExpression(
	    ExpressionLogic expressionLogic )
	{
	    FilterExpression result = null;
	    if ( expressionLogic instanceof ASTAndNode ||
	        expressionLogic instanceof ASTOrNode )
	    {
	        BinaryExpressionNode binaryNode = ( BinaryExpressionNode )
	            expressionLogic;
	        boolean operatorAnd = binaryNode.getOperator().equals( "&&" );
	        result = new FilterBinaryNode(
	            toFilterExpression( binaryNode.getLeftExpression() ),
	            operatorAnd,
	            toFilterExpression( binaryNode.getRightExpression() ) );
	    }
	    else
	    {
	        if ( expressionLogic instanceof ASTGreaterThanEqualsNode ||
	            expressionLogic instanceof ASTEqualsNode ||
	            expressionLogic instanceof ASTGreaterThanNode ||
	            expressionLogic instanceof ASTLessThanEqualsNode ||
	            expressionLogic instanceof ASTLessThanNode )
	        {
	            result = formCompareExpression( expressionLogic );
	        }
	        else if ( expressionLogic instanceof ASTRegexFuncNode )
	        {
	            result = formRegexPattern( expressionLogic );
	        }
	        else
	        {
	            throw new RuntimeException( expressionLogic +
	                " not supported" );
	        }
	    }
	    return result;
	}
	
	private FilterExpression formCompareExpression(
	    ExpressionLogic expressionLogic )
	{
	    BinaryExpressionNode binaryNode =
	        ( BinaryExpressionNode ) expressionLogic;
	    String operator = binaryNode.getOperator();
	    ASTVar var = ( ASTVar ) binaryNode.getLeftExpression();
	    ASTLiteral value = ( ASTLiteral ) binaryNode.getRightExpression();
	    URI datatype = value.getDatatype();
	    Object realValue = null;
	    String stringValue = value.toString();
        if ( XMLDatatypeUtil.isDecimalDatatype( datatype ) ||
            XMLDatatypeUtil.isFloatingPointDatatype( datatype ) )
        {
            realValue = new Double( stringValue );
	    }
        else if ( XMLDatatypeUtil.isIntegerDatatype( datatype ) )
        {
            realValue = new Integer( stringValue );
        }
	    else
	    {
	        realValue = value.getLabel();
	    }
        
        NeoVariable variable = getVariable( var );
        return new CompareExpression( var.getName(), variable.getProperty(),
            operator, realValue );
	}
	
	private FilterExpression formRegexPattern( ExpressionLogic expressionLogic )
	{
        ASTRegexFuncNode regexNode = ( ASTRegexFuncNode ) expressionLogic;
        List<?> arguments = regexNode.getArguments();
        ASTVar variable = ( ASTVar ) arguments.get( 0 );
        ASTLiteral regexValue = ( ASTLiteral ) arguments.get( 1 );
        ASTLiteral regexOptions = arguments.size() > 2 ?
            ( ASTLiteral ) arguments.get( 2 ) : null;
        NeoVariable neoVariable = getVariable( variable );
        return new RegexPattern( variable.getName(),
            neoVariable.getProperty(), regexValue.getLabel(),
            regexOptions == null ? "" : regexOptions.getLabel() );
	}

	private void addOwlProperty( ExpressionLogic subjectExpression,
		ExpressionLogic predicateExpression, ExpressionLogic objectExpression,
		PatternGroup group, boolean optional )
	{
		PatternNode subjectNode = this.getOrCreatePatternNode(
			subjectExpression, group );
		
		OwlProperty property = this.getOwlProperty(
			subjectExpression, predicateExpression, objectExpression );
		
		if ( property.getType() == OwlPropertyType.OBJECT_TYPE )
		{
			subjectNode.createRelationshipTo(
				this.getOrCreatePatternNode( objectExpression, group ),
				( RelationshipType ) property.getMappedValue(), optional );
		}
		else // It's an OwlProperty.DATATYPE_TYPE
		{
			if ( objectExpression instanceof ASTLiteral )
			{
				String propertyKey = ( String ) property.getMappedValue();
				Object valueToMatch =
					metaModel.convertCriteriaStringValueToRealValue(
						propertyKey, ( ( ASTLiteral )
							objectExpression ).getLabel() );
				subjectNode.addPropertyEqualConstraint(
					propertyKey, optional, valueToMatch );
			}
			else if ( objectExpression instanceof ASTVar )
			{
				subjectNode.addPropertyExistConstraint(
					( String ) property.getMappedValue() );
				this.addVariable( ( ASTVar ) objectExpression,
					NeoVariable.VariableType.LITERAL,
					subjectNode, ( String ) property.getMappedValue() );
			}
			else
			{
				throw new QueryException( "Object [" + objectExpression +
				"] should be a literal or a variable." );
			}
		}
	}

	private void addVariable( ASTVar var, VariableType type,
		PatternNode subjectNode, String string )
	{
		for ( NeoVariable variable : this.variableList )
		{
			if ( var.getName().equals( variable.getName() ) )
			{
				return;
			}
		}
		if ( getVariableOrNull( var ) == null )
		{
    		this.variableList.add(
    			new NeoVariable( var, type, subjectNode, string ) );
		}
	}
	
	private NeoVariable getVariableOrNull( ASTVar var )
	{
	    for ( NeoVariable variable : this.variableList )
	    {
	        if ( var.getName().equals( variable.getName() ) )
	        {
	            return variable;
	        }
	    }
	    return null;
	}
	
	private NeoVariable getVariable( ASTVar var )
	{
	    NeoVariable variable = getVariableOrNull( var );
	    if ( variable == null )
	    {
	        throw new RuntimeException( "Undefined variable for " + var );
	    }
	    return variable;
	}

	private OwlProperty getOwlProperty( ExpressionLogic subjectExpression,
		ExpressionLogic predicateExpression, ExpressionLogic objectExpression )
	{
		if ( objectExpression instanceof ASTVar )
		{
			return this.metaModel.getOwlProperty(
				this.classMapping.get( subjectExpression ),
				this.toUri( predicateExpression ),
				this.classMapping.get( objectExpression ) );
		}
		else
		{
			return this.metaModel.getOwlProperty(
				this.classMapping.get( subjectExpression ),
				this.toUri( predicateExpression ), null );
		}
	}

	private PatternNode getOrCreatePatternNode( ExpressionLogic expression,
	    PatternGroup group )
	{
		return getOrCreatePatternNode( expression, group, false );
	}
	
	private PatternNode getOrCreatePatternNode( ExpressionLogic expression,
		PatternGroup group, boolean isClass )
	{
		return getOrCreatePatternNode( expression, group, isClass, null );
	}
	
	private PatternNode getOrCreatePatternNode( ExpressionLogic expression,
		PatternGroup group, boolean isClass, RunOnPatternNode runOnCreation )
	{
		PatternNode node = this.graph.get( expression );
		if ( node == null )
		{
			node = this.createPatternNode( expression, group, isClass );
			if ( runOnCreation != null )
			{
				runOnCreation.onCreated( node );
			}
		}
		return node;
	}
	
	private PatternNode getOrCreatePatternNode( String nodeType )
	{
		PatternNode node = this.nodeTypes.get( nodeType );
		if ( node == null )
		{
			node = new PatternNode( nodeType );
			this.nodeTypes.put( nodeType, node );
			this.possibleStartNodes.add( node );
		}
		return node;
	}

	private PatternNode createPatternNode(
		ExpressionLogic expression, PatternGroup group, boolean isClass )
	{
		PatternNode node =
			new PatternNode( group, this.toUri( expression ) );
		this.graph.put( expression, node );
		
		if ( expression instanceof ASTQName ||
			expression instanceof ASTQuotedIRIref )
		{
			this.possibleStartNodes.add( node );
			if ( !isClass )
			{
				node.addPropertyEqualConstraint( this.metaModel.getAboutKey(),
					this.toUri( expression ) );
			}
		}
		
		if ( expression instanceof ASTVar )
		{
			String key = isClass ? this.metaModel.getNodeTypeNameKey() :
				this.metaModel.getAboutKey();
			this.addVariable( ( ASTVar ) expression,
				NeoVariable.VariableType.URI, node, key );
		}
		
		return node;
	}
	
	private void assertConstraint( TripleConstraint constraint )
	{
		if ( !( constraint.getPredicateExpression() instanceof ASTQName ) )
		{
			throw new QueryException(
				"Predicate [" + constraint.getPredicateExpression() +
				"] is not a fully qualified predicate. Not supported." );
		}
	}

	private String toUri( ExpressionLogic expression )
	{
		String namespace = "";
		String localName = "";
		
		if ( expression instanceof URINode )
		{
			namespace = ( ( URINode ) expression ).getNamespace();
			localName = ( ( URINode ) expression ).getLocalName();
		}
		else if ( expression instanceof ASTVar )
		{
			localName = ( ( ASTVar ) expression ).getName();
		}
		else if ( expression instanceof ASTLiteral )
		{
			localName = ( ( ASTLiteral ) expression ).getLabel();
		}
		
		return namespace + localName;
	}

	private interface RunOnPatternNode
	{
		void onCreated( PatternNode node );
	}
	
	private final RunOnPatternNode ON_CREATED_TYPE =
		new RunOnPatternNode()
	{
		public void onCreated( PatternNode node )
		{
			node.addPropertyEqualConstraint( metaModel.getNodeTypeNameKey(),
				metaModel.getSubTypes( node.getLabel(), true ) );
		}
	};

	void assertGraph()
	{
		for ( PatternNode node : this.graph.values() )
		{
			if ( !this.possibleStartNodes.contains( node ) )
			{
				this.assertHasTypeRelationship( node );
			}
		}
	}
	
	private PatternRelationship findTypeRelationshipOrNull( PatternNode node )
	{
		for ( PatternRelationship relationship : node.getAllRelationships() )
		{
			if ( relationship.getType().equals(
				this.metaModel.getTypeRelationship() ) )
			{
				return relationship;
			}
		}
		return null;
	}
	
	private void assertHasTypeRelationship( PatternNode node )
	{
		boolean found = findTypeRelationshipOrNull( node ) != null;
		if ( !found )
		{
			found = tryToFindAndAddType( node );
		}
		if ( !found )
		{
			throw new QueryException( "Type for variable [" + node.getLabel() +
				"] is not specified. Not supported." );
		}
	}
	
	private boolean tryToFindAndAddType( PatternNode nodeWithMissingType )
	{
		// Try to get the type from proxy...
		// allright take the first relationship for this node,
		// go to that other node and get its type relationship to
		// use you know!
		PatternRelationship firstRelationshipHack = null;
		for ( PatternRelationship rel : nodeWithMissingType.getAllRelationships() )
		{
			firstRelationshipHack = rel;
			break;
		}
		
		if ( firstRelationshipHack != null )
		{
			PatternNode otherNode =
				firstRelationshipHack.getOtherNode( nodeWithMissingType );
			PatternRelationship otherNodesTypeRel =
				findTypeRelationshipOrNull( otherNode );
			if ( otherNodesTypeRel != null )
			{
				PatternNode otherTypeNode = otherNodesTypeRel.getOtherNode(
					otherNode );
				String type = this.metaModel.getObjectType(
					otherTypeNode.getLabel(),
					firstRelationshipHack.getType().name() );
				if ( type != null )
				{
					PatternNode typeNode = getOrCreatePatternNode( type );
					nodeWithMissingType.createRelationshipTo( typeNode,
						this.metaModel.getTypeRelationship() );
					return true;
				}
			}
		}
		return false;
	}
}
