package org.neo4j.weaver.impl;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.lang.reflect.Method;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sf.cglib.proxy.MethodProxy;

import org.neo4j.api.core.Node;
import org.neo4j.weaver.api.DomainObjectFactory;
import org.neo4j.weaver.impl.NodeAware;
import org.neo4j.weaver.impl.NodeProxyInterceptor;


public class NodeProxyInterceptorTest extends TestCase {
	// Class under test
	private NodeProxyInterceptor interceptor;
	// Mock objects
	private Node nodeMock;
	private DomainObjectFactory factoryMock;
	private MethodProxy methodProxyMock;

	public void setUp() {
		nodeMock = createMock(Node.class);
		factoryMock = createMock(DomainObjectFactory.class);
		methodProxyMock = createMock(MethodProxy.class);
		interceptor = new NodeProxyInterceptor(nodeMock, SomeInterface.class, factoryMock);
	}

	public void tearDown() {
		nodeMock = null;
		factoryMock = null;
		interceptor = null;
	}

	public void replayAll() {
		replay(nodeMock);
		replay(factoryMock);
		replay(methodProxyMock);
	}
	
	public void verifyAll() {
		verify(nodeMock);
		verify(factoryMock);
		verify(methodProxyMock);
	}

	public void testGetUnderlyingNode() throws Throwable {
		SomeInterface obj = new SomeInterfaceProxy();

		Method method = SomeInterfaceProxy.class.getMethod("getUnderlyingNode", new Class<?>[0]);

		replayAll();

		Object rv = interceptor.intercept(obj, method, new Object[0], methodProxyMock);

		assertEquals("Expected Node", nodeMock, rv);
		
		verifyAll();
	}
	
	public void testMixinInvocation() throws Throwable {
		SomeInterface obj = new SomeInterfaceProxy();

		Method method = SomeInterfaceImpl.class.getMethod("getSomething", new Class<?>[0]);

		expect(methodProxyMock.invokeSuper(eq(obj), aryEq(new Object[0]))).andReturn("SomeReturnValue");

		replayAll();

		interceptor.intercept(obj, method, new Object[0], methodProxyMock);

		verifyAll();
	}

	public void testProxyInvocation() throws Throwable {
		SomeInterface obj = new SomeInterfaceProxy();

		Method method = SomeInterfaceImpl.class.getMethod("getSomethingElse", new Class<?>[0]);

		expect(nodeMock.getProperty("SomethingElse")).andReturn("SomeReturnValue");

		replayAll();

		interceptor.intercept(obj, method, new Object[0], methodProxyMock);

		verifyAll();
	}
	
	interface SomeInterface {
		public int getSomething();
		public int getSomethingElse();
	}
	
	abstract class SomeInterfaceImpl implements SomeInterface {
		public int getSomething() {
			throw new AssertionFailedError("This method should never be called; intercepted by proxy");
		}
	}
	
	class SomeInterfaceProxy extends SomeInterfaceImpl implements NodeAware {
		public int getSomethingElse() {
			throw new AssertionFailedError("This method should never be called; intercepted by proxy");
		}

		public Node getUnderlyingNode() {
			return nodeMock;
		}
	}
}