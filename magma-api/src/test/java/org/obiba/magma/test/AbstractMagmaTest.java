package org.obiba.magma.test;

import org.junit.After;
import org.junit.Before;
import org.obiba.magma.MagmaEngine;

/**
 * This class is a copy of the original in magma-test, so we don' have circular dependency of
 * magma-api -> magma-test -> magma-api
 */
public abstract class AbstractMagmaTest {

  @Before
  public void before() {
    new MagmaEngine();
  }

  @After
  public void after() {
    MagmaEngine.get().shutdown();
  }
}
