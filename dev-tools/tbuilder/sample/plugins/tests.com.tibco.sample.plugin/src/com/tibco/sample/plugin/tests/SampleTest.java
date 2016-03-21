package com.tibco.sample.plugin.tests;

import com.tibco.sample.plugin.Dummy;

import junit.framework.TestCase;

/** Currently extremely bogus.
*/

public class SampleTest extends TestCase
{
    public void testSample()
    {
        Dummy dummy = new Dummy("for testing");
        assertNotNull(dummy);
    }
}