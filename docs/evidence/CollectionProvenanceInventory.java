import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.*;
import java.util.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** One streaming production walk per physical card, at the normal 1 GiB heap. */
public class CollectionProvenanceInventory {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
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
