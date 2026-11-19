import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.explorer.ResearchProgressService;
import dev.mtg.assistant.audit.CoverageReport;
import dev.mtg.assistant.discovery.EvidenceKt;
import kotlinx.serialization.json.JvmStreamsKt;
import java.nio.file.*;
public class ExportProgressAudit {
 public static void main(String[] args) throws Exception {
  var audit=new ResearchProgressService(new Catalog(),"allocation-equivalence",null).getNativeCoverage();
  try(var out=Files.newOutputStream(Path.of(args[0]),StandardOpenOption.CREATE_NEW)) {
   JvmStreamsKt.encodeToStream(EvidenceKt.getReportJson(),CoverageReport.Companion.serializer(),audit,out);
  }
  System.out.println("Exported full native audit: "+audit.getInputCards()+" physical cards, "+audit.getEncounteredTypedNodes()+" typed nodes, "+audit.getGroups().size()+" groups");
 }
}
