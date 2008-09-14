package org.neo4j.weaver.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import net.sf.cglib.proxy.MethodProxy;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.weaver.api.DomainObjectFactory;
import org.neo4j.weaver.api.FindRelationship;

/**
 * The interceptor for all domain object invocations created by the
 * CGLibDomainObjectFactory.
 * 
 * @author Magnus Robertsson
 */
class NodeProxyInterceptor implements net.sf.cglib.proxy.MethodInterceptor {

	private final Node underlyingNode;
	private final Class<?> type;
	private final DomainObjectFactory factory;
	
	NodeProxyInterceptor(Node underlyingNode, Class<?> type, DomainObjectFactory factory) {
		this.underlyingNode = underlyingNode;
		this.type = type;
		this.factory = factory;
	}
	
	@SuppressWarnings("unchecked")
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		String methodName = method.getName();
		if (methodName.equals("getUnderlyingNode")) {
			return underlyingNode;
		} else if (isImplementationPresent(method)) {
			return proxy.invokeSuper(obj, args);
		} else if (methodName.startsWith("get")) {
			AnnotationInterceptor interceptor = assembleAnnotationInterceptor(method);
			if (interceptor == null) {
				String propertyName = methodName.substring(3);
				return underlyingNode.getProperty(propertyName);
			} else {
				return interceptor.invoke(obj, method, args);
			}
		} else if (methodName.startsWith("set")) {
			String propertyName = methodName.substring(3);
			underlyingNode.setProperty(propertyName, args[0]);
		} else if (methodName.equals("equals")) {
			Object otherProxy = args[0];
	        if (type.isInstance(otherProxy)) {
	        	// We could get rid of these xAware interfaces if we inject
	        	// the node to the proxy object instead of the interceptor
	        	// as it works today
	            return this.underlyingNode.equals(((NodeAware) otherProxy).getUnderlyingNode());
	        }
	        return false;
		} else if (methodName.equals("hashCode")) {
			return underlyingNode.hashCode();
		} else {
			throw new UnknownMappingException();
		}

		return null;
	}

	private AnnotationInterceptor assembleAnnotationInterceptor(Method method) {
		FindRelationship annotation = method.getAnnotation(FindRelationship.class);
		if (annotation == null) {
			return null;
		}
		return new FindRelationshipInterceptor(annotation);
	}

	private boolean isImplementationPresent(Method method) {
		return !Modifier.isAbstract(method.getModifiers());
	}
	
	private Class<?> findIterableType(Method method) {
		if (Iterable.class.isAssignableFrom(method.getReturnType())) {
			Type type = method.getGenericReturnType();
			if (type instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) type;
				Type[] ts = paramType.getActualTypeArguments();
				if (ts.length == 1) {
					Type t = ts[0];
					if (t instanceof Class) {
						return (Class<?>) t;
					} else {
						throw new IllegalArgumentException("Only standard classes can be used as parameterized type");
					}
				} else {
					throw new IllegalArgumentException("Only one parameterized type is currently supported");
				}
			}
		}
		return null;
	}

	// Embryo to a framework for intercepting method calls specified by annotations
	interface AnnotationInterceptor {
		public Object invoke(Object obj, Method method, Object[] args);
	}
	
	class FindRelationshipInterceptor implements AnnotationInterceptor {
		private FindRelationship annotation;
		
		public FindRelationshipInterceptor(FindRelationship annotation) {
			this.annotation = annotation;
		}
		
		@SuppressWarnings("unchecked")
		public Object invoke(Object obj, Method method, Object[] args) {
			Class<?> returnType = findIterableType(method);
	        final List list = new LinkedList();
	        final SoftRelationshipType relationshipType = new SoftRelationshipType(annotation.type());
	        for (Relationship rel : underlyingNode.getRelationships(relationshipType, annotation.direction())) {
	        	// TODO Investigate if this is always the case, i.e. finding
	        	// OUTGOING creates end node and vice versa
	        	// If not, we might need to add another annotation parameter
	        	if (annotation.direction().equals(Direction.OUTGOING)) {
		            list.add(factory.create(rel.getEndNode(), returnType));
	        	} else {
		            list.add(factory.create(rel.getStartNode(), returnType));
	        	}
	        }
	        return list;
		}
	}
	
	private class SoftRelationshipType implements RelationshipType {
		private final String name;
		
		private SoftRelationshipType(String name) {
			this.name = name;
		}
		
		public String name() {
			return name;
		}
		
	}
}

