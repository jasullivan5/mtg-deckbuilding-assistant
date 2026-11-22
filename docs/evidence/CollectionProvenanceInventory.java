import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.*;
import java.util.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** One streaming production walk per physical card, at the normal 1 GiB heap. */
public class CollectionProvenanceInventory {
    static String quote(String value) {
        return "\""+value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";
    }
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        // Supplement the existing native-only census without scanning the whole catalog again.
        if (Arrays.asList(args).contains("--extensions")) {
            for (var extension : dev.mtg.assistant.extensions.Extensions.INSTANCE.getAll()) {
                var card=extension.getDefinition();
                if (card.getBackFace()!=null || card.getFlipSide()!=null || !card.getCardFaces().isEmpty())
                    throw new IllegalStateException("Extend the face-aware extension census before counting this layout");
                String script=EvidenceKt.getNativeJson().encodeToString(com.wingedsheep.sdk.model.CardScript.Companion.serializer(),card.getScript());
                System.out.println("{\"physical\":"+quote(card.getName())+",\"face\":\"front\",\"nativePath\":"+quote(extension.getProvenance().getSource())+",\"script\":"+script+"}");
            }
            return;
        }
        var catalog = new Catalog();
        for (var card : EligibilityKt.physicalCards(catalog.getCards())) {
            var extracted = new PathwayExtractor().extract(List.of(card));
            for (var action : extracted.getActions()) {
                if (action.getUnionTargets().isEmpty() && action.getCollectionStages().isEmpty() && action.getCollectionUses().isEmpty()) continue;
                System.out.println(EvidenceKt.getReportJson().encodeToString(ActionEvidence.Companion.serializer(), action));
            }
        }
    }
}
