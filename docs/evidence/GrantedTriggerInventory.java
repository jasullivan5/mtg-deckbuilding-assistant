import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.*;
import java.util.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** One production walk per integrated physical card at the normal one-GiB heap. */
public class GrantedTriggerInventory {
    static String quote(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
    static void types(com.wingedsheep.sdk.model.CardDefinition card,String path,Map<String,com.wingedsheep.sdk.core.TypeLine> result) {
        result.put(path,card.getTypeLine());
        if(card.getBackFace()!=null)types(card.getBackFace(),path+".backFace",result);
        if(card.getFlipSide()!=null)types(card.getFlipSide(),path+".flipSide",result);
        for(int i=0;i<card.getCardFaces().size();i++) {
            var face=card.getCardFaces().get(i);
            result.put(path+".cardFaces["+i+"]:"+face.getName(),face.getTypeLine());
        }
    }
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out,true,StandardCharsets.UTF_8));
        var catalog=new Catalog();
        var sources=DefinitionSources.INSTANCE.getPaths();
        for(var card:EligibilityKt.physicalCards(catalog.getCards())) {
            var extracted=new PathwayExtractor().extract(List.of(card));
            var types=new HashMap<String,com.wingedsheep.sdk.core.TypeLine>();
            types(card,"front",types);
            for(var root:extracted.getActions()) {
                if(!root.getPath().equals("script")) continue;
                var grants=new ArrayList<String>();
                for(var action:extracted.getActions()) if(action.getFace().equals(root.getFace()))
                    for(var grant:action.getGrantedTriggers())
                        grants.add(EvidenceKt.getReportJson().encodeToString(GrantedTrigger.Companion.serializer(),grant));
                var source=sources.get(card.getName());
                if(source==null)source=catalog.provenance(card.getName()).getSource();
                var type=Objects.requireNonNull(types.get(root.getFace()));
                System.out.println("{\"physical\":"+quote(card.getName())+",\"face\":"+quote(root.getFace())+",\"source\":"+quote(source)+",\"aura\":"+type.isAura()+",\"equipment\":"+type.isEquipment()+",\"script\":"+root.getNative()+",\"grants\":["+String.join(",",grants)+"]}");
            }
        }
    }
}
