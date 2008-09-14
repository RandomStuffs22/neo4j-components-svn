package org.neo4j.weaver.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.sf.cglib.proxy.MethodProxy;

import org.neo4j.api.core.Relationship;
import org.neo4j.weaver.api.DomainObjectFactory;
import org.neo4j.weaver.api.EndNode;
import org.neo4j.weaver.api.StartNode;


class RelationshipProxyInterceptor implements net.sf.cglib.proxy.MethodInterceptor {

	private final Relationship underlyingRelationship;
	private final Class<?> type;
	private final DomainObjectFactory factory;
	
	RelationshipProxyInterceptor(Relationship underlyingRelationship, Class<?> type, DomainObjectFactory factory) {
		this.underlyingRelationship = underlyingRelationship;
		this.type = type;
		this.factory = factory;
	}
	
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		String methodName = method.getName();
		if (methodName.equals("getUnderlyingRelationship")) {
			return underlyingRelationship;
		} else if (isImplementationPresent(method)) {
			return proxy.invokeSuper(obj, args);
		} else if (methodName.startsWith("get")) {
			if (method.isAnnotationPresent(StartNode.class)) {
		        return factory.create(underlyingRelationship.getStartNode(), method.getReturnType());
			} else if (method.isAnnotationPresent(EndNode.class)) {
		        return factory.create(underlyingRelationship.getEndNode(), method.getReturnType());
			} else {
				String propertyName = methodName.substring(3);
		        if (underlyingRelationship.hasProperty(propertyName)) {
					return underlyingRelationship.getProperty(propertyName);
		        } else {
		        	return null;
		        }
			}
		} else if (methodName.startsWith("set")) {
			String propertyName = methodName.substring(3);
			underlyingRelationship.setProperty(propertyName, args[0]);
		} else if (methodName.equals("equals")) {
			Object otherProxy = args[0];
	        if (type.isInstance(otherProxy)) {
	            return this.underlyingRelationship.equals(((RelationshipAware) otherProxy).getUnderlyingRelationship());
	        }
	        return false;
		} else if (methodName.equals("hashCode")) {
			return underlyingRelationship.hashCode();
		} else if (type.isInterface()) {
			throw new UnknownMappingException();
		} else {
			// Try to invoke method
			// TODO Check that an implementation really exist
			return proxy.invokeSuper(obj, args);
		}

		return null;
	}

	private boolean isImplementationPresent(Method method) {
		return !Modifier.isAbstract(method.getModifiers());
	}
}
