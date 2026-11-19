import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.MainKt;
import java.nio.file.*;
import java.util.*;
import kotlin.Unit;
/** Normal production catalog on both sides; no replacement definitions or correction-plan bypass. */
public class CompareProductionPopulation {
  public static void main(String[] args) throws Exception {
    Path output=Path.of(args[0]);Files.createDirectories(output);
    Catalog catalog=new Catalog();
    for(String resource:List.of("application-build.properties","engine-build.properties")) {
      try(var in=CompareProductionPopulation.class.getResourceAsStream("/"+resource)) {
        Files.copy(Objects.requireNonNull(in),output.resolve(resource));
      }
    }
    String[] objectives={"own-nontoken-creature-deaths","reanimate-own-creatures","create-creature-tokens","creature-died-this-turn","stock-own-graveyard","place-plus-one-counters"};
    for(String commander:List.of("Judith, the Scourge Diva","Krenko, Mob Boss","Kenrith, the Returned King")) {
      String slug=commander.substring(0,commander.indexOf(',' )).toLowerCase();
      for(String objective:objectives) {
        String[] cli={"discover","--commander",commander,"--objective",objective,"--json","--output",output.resolve(slug+"-"+objective+".json").toString()};
        int status=MainKt.runCli(cli,()->catalog,line->{System.out.println(line);return Unit.INSTANCE;},line->{System.err.println(line);return Unit.INSTANCE;});
        if(status!=0)throw new IllegalStateException("Discovery failed: "+commander+" / "+objective+" status="+status);
      }
    }
  }
}
