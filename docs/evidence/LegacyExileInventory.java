import com.wingedsheep.sdk.model.CardDefinition;
import com.wingedsheep.sdk.model.CardScript;
import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.EligibilityKt;
import dev.mtg.assistant.discovery.EvidenceKt;
import java.util.*;

public class LegacyExileInventory {
    static void emit(CardDefinition physical, CardDefinition face, String path) {
        String script=EvidenceKt.getNativeJson().encodeToString(CardScript.Companion.serializer(),face.getScript());
        // JSON strings use the SDK serializer; this wrapper contains only stable identity fields.
        System.out.println("{\"nativePath\":"+quote(dev.mtg.assistant.discovery.DefinitionSources.INSTANCE.getPaths().get(physical.getName()))+",\"physical\":"+quote(physical.getName())+",\"face\":"+quote(path)+",\"name\":"+quote(face.getName())+",\"oracleId\":"+quote(physical.getOracleId())+",\"oracleText\":"+quote(face.getOracleText())+",\"script\":"+script+"}");
        if(face.getBackFace()!=null)emit(physical,face.getBackFace(),path+".backFace");
        if(face.getFlipSide()!=null)emit(physical,face.getFlipSide(),path+".flipSide");
        for(int i=0;i<face.getCardFaces().size();i++) {
            var f=face.getCardFaces().get(i);
            var text=EvidenceKt.getNativeJson().encodeToString(CardScript.Companion.serializer(),f.getScript());
            System.out.println("{\"nativePath\":"+quote(dev.mtg.assistant.discovery.DefinitionSources.INSTANCE.getPaths().get(physical.getName()))+",\"physical\":"+quote(physical.getName())+",\"face\":"+quote(path+".cardFaces["+i+"]")+",\"name\":"+quote(f.getName())+",\"oracleId\":"+quote(physical.getOracleId())+",\"oracleText\":"+quote(f.getOracleText())+",\"script\":"+text+"}");
        }
    }
    static String quote(String s){
        if(s==null)return "null";
        return '"'+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+'"';
    }
    public static void main(String[] args){
        System.setOut(new java.io.PrintStream(System.out,true,java.nio.charset.StandardCharsets.UTF_8));
        Catalog c=new Catalog(List.of(),()->null);
        for(var card:EligibilityKt.physicalCards(c.getCards()))emit(card,card,"front");
    }
}


