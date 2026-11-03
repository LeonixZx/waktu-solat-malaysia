package com.example.waktusolatmalaysia.repo;
import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;

public class ReadingParserTest {
  @Test public void allSuppliedReadingsHaveStableIdsAndLabelledPartsInBothLanguages() throws Exception {
    String[] kinds={"doa","wirid","niat","hadith"}, files={"prayer_guides.json","wirid.json","prayer_intentions.json","hadiths.json"};
    int[] expected={5,14,5,12};
    for (boolean english : new boolean[]{false,true}) {
      Set<String> ids=new HashSet<>();
      for (int k=0;k<kinds.length;k++) {
        var rows=ReadingParser.parse(kinds[k],new String(Files.readAllBytes(Paths.get("src/main/assets/"+files[k])), java.nio.charset.StandardCharsets.UTF_8),english);
        assertEquals(expected[k],rows.size());
        for(int i=0;i<rows.size();i++) {
          var row=rows.get(i); assertEquals(kinds[k]+":"+i,row.id); assertTrue(ids.add(row.id));
          assertFalse(row.title.isBlank()); assertFalse(row.reference.isBlank()); assertFalse(row.sections.isEmpty());
          for(var section:row.sections) {
            assertFalse(section.arabic.isBlank()); assertFalse(section.translation(english).isBlank());
            assertTrue(Set.of("ms","en","id").contains(section.language(english)));
            if(kinds[k].equals("hadith")&&!english) assertEquals("id",section.language(false));
          }
        }
      }
      assertEquals(36,ids.size());
    }
  }
  @Test public void romanTextIsSeparateAndMissingEnglishUsesLabelledMalay() throws Exception {
    var r=ReadingParser.parse("wirid",new String(Files.readAllBytes(Paths.get("src/main/assets/wirid.json")), java.nio.charset.StandardCharsets.UTF_8),true).get(0);
    var section=r.sections.get(0); assertFalse(section.transliteration.isBlank());
    assertTrue(section.englishTranslation.isEmpty()); assertEquals("ms",section.language(true));
    assertEquals(section.localTranslation,r.meaning); assertFalse(r.meaning.contains(section.transliteration));
  }
}
