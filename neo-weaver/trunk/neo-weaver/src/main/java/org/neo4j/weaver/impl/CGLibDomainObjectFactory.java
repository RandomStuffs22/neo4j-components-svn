package org.neo4j.weaver.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.neo4j.weaver.api.DomainObjectFactory;
import org.neo4j.weaver.api.Mixin;

/**
 * Spring friendly implementation of the DomainObjectFactory that uses CGLib
 * to create proxy objects.
 * 
 * @author Magnus Robertsson
 */
public class CGLibDomainObjectFactory implements DomainObjectFactory {

    private final Map<Class<?>,Class<?>> implTypes = new HashMap<Class<?>,Class<?>>();
	private NeoService neoService;
    
	public CGLibDomainObjectFactory() {
	}

	public CGLibDomainObjectFactory(NeoService neoService) {
		this.neoService = neoService;
	}
	
    // Used for Spring configuration
    public void setNeoService(NeoService neoService) {
    	this.neoService = neoService;
    }
    
    // Used for Spring configuration
    public void setMixins(Map<Class<?>,Class<?>> domainToMixinTypes) {
    	implTypes.clear();
    	for (Map.Entry<Class<?>,Class<?>> entry : domainToMixinTypes.entrySet()) {
    		addMixin(entry.getKey(), entry.getValue());
    	}
    }

    /**
     * Adds a mixin type for a domain object.
     * 
     * @param domainType The domain interface.
     * @param mixinType  The abstract mixin class that implements or extends
     *                   the domain interface.
     */
    public void addMixin(Class<?> domainType, Class<?> mixinType) {
		// Check that the implType really implement or extend interfaceType
		if (!domainType.isAssignableFrom(mixinType)) {
			throw new IllegalArgumentException("The Mixin type " + mixinType + " does not implement or extend " + domainType);
		}
    	implTypes.put(domainType, mixinType);
    }
    
	public <T> T create(Class<T> type) {
		Node node = neoService.createNode();
		return create(node, type);
	}
	
	public <T> T create(Node underlyingNode, Class<T> type) {
		NodeProxyInterceptor interceptor = new NodeProxyInterceptor(
				underlyingNode,
				type,
				this);
		return createProxy(interceptor, type);
	}

	public <T> T create(Relationship underlyingRelationship, Class<T> type) {
		RelationshipProxyInterceptor interceptor = new RelationshipProxyInterceptor(
				underlyingRelationship,
				type,
				this);
		return createProxy(interceptor, type);
	}

	@SuppressWarnings("unchecked")
	private <T> T createProxy(MethodInterceptor interceptor, Class<T> type) {
		Class<?> proxyType = resolveProxyType(type);
		
		T proxy = null;
		if (proxyType.isInterface()) {
			Enhancer enhancer = new Enhancer();
			enhancer.setInterfaces(new Class[] { proxyType, NodeAware.class });
			enhancer.setCallback(interceptor);
			proxy = (T) enhancer.create();
		} else {
			proxy = (T) Enhancer.create(proxyType, new Class[] { NodeAware.class }, interceptor);
		}

		injectProxyFactory(proxyType, proxy);

		return proxy;
	}

	private Class<?> resolveProxyType(Class<?> type) {
		Class<?> proxyType = implTypes.get(type);
		if (proxyType == null) {
			Mixin mixinAnnotation = type.getAnnotation(Mixin.class);
			if (mixinAnnotation == null) {
				proxyType = type;
			} else {
				proxyType = mixinAnnotation.value();
			}
			// Cache type for faster access
			addMixin(type, proxyType);
		}
		
		return proxyType;
	}

	// Inject ProxyFactory if such field exists
	private void injectProxyFactory(Class<?> proxyType, Object proxy) {
		Field proxyFactoryField = getProxyFactoryField(proxyType);
		if (proxyFactoryField != null) {
			proxyFactoryField.setAccessible(true);
			try {
				proxyFactoryField.set(proxy, this);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	// Returns the first field of type ProxyFactory
	private Field getProxyFactoryField(Class<?> type) {
		Field[] fields = type.getDeclaredFields();
		for (Field field : fields) {
			if (field.getType().equals(DomainObjectFactory.class)) {
				return field;
			}
		}
		return null;
	}
	
	public Node getUnderlyingNode(Object obj) {
		if (obj instanceof NodeAware) {
			return ((NodeAware) obj).getUnderlyingNode();
		} else {
			throw new IllegalArgumentException("Not an object created by this ProxyFactory");
		}
	}
}
