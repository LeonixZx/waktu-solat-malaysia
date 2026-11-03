package com.example.waktusolatmalaysia;
import static org.junit.Assume.assumeTrue;
import static org.junit.Assert.assertTrue;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;

/** Explicit test cleanup; changes only the collection tab supplied from the pre-test snapshot. */
public class TestDataRestore {
  @Test public void restoreTemporaryCollectionTab() {
    String value=InstrumentationRegistry.getArguments().getString("restoreCategory");
    assumeTrue(value!=null && java.util.Set.of("doa","wirid","niat","hadith").contains(value));
    assertTrue(InstrumentationRegistry.getInstrumentation().getTargetContext()
        .getSharedPreferences("personal_library",0).edit().putString("collection_tab",value).commit());
  }
}
