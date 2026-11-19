import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.MainKt;
import kotlin.Unit;
import java.nio.file.*;
/** Read-only historical report compatibility using the production CLI's audit implementation. */
public class AuditJdk25 {
  public static void main(String[] args) throws Exception {
    Path before=Path.of(args[0]),after=Path.of(args[1]),out=Path.of(args[2]);
    Files.createDirectories(out);
    int count=0;
    try(var paths=Files.list(after)) {
      for(Path p:paths.filter(p->p.toString().endsWith(".json")).sorted().toList()) {
        String[] cli={"audit","diff","--before",before.resolve(p.getFileName()).toString(),"--after",p.toString(),"--json","--output",out.resolve(p.getFileName()).toString()};
        int status=MainKt.runCli(cli,Catalog::new,line->{System.out.println(line);return Unit.INSTANCE;},line->{System.err.println(line);return Unit.INSTANCE;});
        if(status!=0)throw new IllegalStateException("Historical audit failed: "+p+" status="+status);
        count++;
      }
    }
    if(count!=18)throw new IllegalStateException("Expected 18 audits, got "+count);
    System.out.println("Completed all 18 historical audits");
  }
}
