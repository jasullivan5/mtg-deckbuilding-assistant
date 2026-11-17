import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.MainKt;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
/** Explicit native-only diagnostic, never the normal application catalog. */
public class CompareNativePopulation {
  public static void main(String[] args) throws Exception {
    Path output=Path.of(args[0]);Files.createDirectories(output);
    Catalog catalog=new Catalog(Collections.emptyList(),()->null);
    String commander="Krenko, Mob Boss";
    String[] objectives={"own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens","creature-died-this-turn","stock-own-graveyard","place-plus-one-counters"};
    for(String objective:objectives) {
      String[] cli={"discover","--commander",commander,"--objective",objective,"--json","--output",output.resolve("krenko-"+objective+".json").toString()};
      int status=MainKt.runCli(cli,()->catalog,line->{System.out.println(line);return Unit.INSTANCE;},line->{System.err.println(line);return Unit.INSTANCE;});
      if(status!=0)throw new IllegalStateException("Diagnostic discovery failed: "+objective+" status="+status);
    }
  }
}
