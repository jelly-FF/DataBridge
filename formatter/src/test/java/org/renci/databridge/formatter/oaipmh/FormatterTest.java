package org.renci.databridge.formatter.oaipmh;

import java.util.List;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.matchers.JUnitMatchers;
import org.junit.Rule;

import org.renci.databridge.formatter.MetadataFormatter;
import org.renci.databridge.formatter.oaipmh.*;

/**
 * Test OaipmhMetadataFormatterImpl.
 */
public class FormatterTest {

    @Test
    public void testUnmarshalAnOaiPmh () throws Exception {

      System.out.println ("Testing unmarshal OAI-PMH...");

      StringWriter sw = new StringWriter ();

      try (InputStream is = getClass ().getResourceAsStream ("/OAI_Odum_Harris.xml")) {
        int c;
        while ((c = is.read ()) != -1 ) {
          sw.write (c);
        }
      }

      OaipmhMetadataFormatterImpl omfi = new OaipmhMetadataFormatterImpl ();

      OAIPMHtype ot = omfi.unmarshal (sw.toString ());
 
      TestCase.assertTrue ("Returned object is null",  ot != null);

      ListRecordsType lrt = ot.getListRecords ();
      List<RecordType> lr = lrt.getRecord ();
      RecordType r = lr.get (0);
      HeaderType h = r.getHeader ();
      String i = h.getIdentifier ();

      TestCase.assertTrue ("Record identifier is incorrect.", "Harris//hdl:1902.29/H-15085".equals (i));

    }
 
    // @todo test "format" method

    @Rule
    public ExpectedException thrown = ExpectedException.none();

}

