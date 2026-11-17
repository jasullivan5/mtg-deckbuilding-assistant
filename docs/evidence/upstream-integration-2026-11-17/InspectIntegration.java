import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.extensions.Extensions;
import dev.mtg.assistant.extensions.ExtensionDefinition;
import com.wingedsheep.sdk.model.CardDefinition;
import com.wingedsheep.sdk.tooling.CardExporter;
import java.nio.file.*;
import java.util.*;

/** One-off, read-only catalog inventory. It selects no definition and edits no evidence. */
public class InspectIntegration {
  public static void main(String[] args) throws Exception {
    Path output=Path.of(args[0]); Files.createDirectories(output);
    Catalog nativeCatalog=new Catalog(Collections.emptyList(), () -> null);
    var inventory=new ArrayList<String>();
    inventory.add("name\tapplicationOracleId\tnativeOracleId\tnativeSet\tnativeCollector\tnativeProvenance");
    for (ExtensionDefinition extension:Extensions.INSTANCE.getAll()) {
      CardDefinition authored=extension.getDefinition();
      CardDefinition nativeCard=nativeCatalog.card(authored.getName());
      if(nativeCard==null) continue;
      String slug=authored.getName().replaceAll("[^A-Za-z0-9]+","_");
      Files.writeString(output.resolve(slug+"-authored-current-sdk.json"),CardExporter.INSTANCE.exportToJson(authored));
      Files.writeString(output.resolve(slug+"-native.json"),CardExporter.INSTANCE.exportToJson(nativeCard));
      inventory.add(String.join("\t",authored.getName(),String.valueOf(authored.getOracleId()),String.valueOf(nativeCard.getOracleId()),String.valueOf(nativeCard.getSetCode()),String.valueOf(nativeCard.getMetadata().getCollectorNumber()),nativeCatalog.provenance(authored.getName()).getOrigin()));
    }
    Files.write(output.resolve("overlaps.tsv"),inventory);
    System.out.println("Native cards="+nativeCatalog.getCards().size()+"; overlapping authored definitions="+(inventory.size()-1));
    try { new Catalog(); throw new AssertionError("Default catalog unexpectedly admitted overlaps"); }
    catch(IllegalArgumentException expected) { Files.writeString(output.resolve("default-catalog-refusal.txt"),expected.toString()); System.out.println(expected); }
  }
}
