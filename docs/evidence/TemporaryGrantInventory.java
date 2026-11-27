import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.*;
import java.util.List;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Streams the production walk and its exact native scripts at the normal 1 GiB heap. */
public class TemporaryGrantInventory {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        var catalog = new Catalog();
        for (var card : EligibilityKt.physicalCards(catalog.getCards())) {
            var extracted = new PathwayExtractor().extract(List.of(card));
            for (var action : extracted.getActions()) {
                if (action.getPath().equals("script") || !action.getTemporaryGrants().isEmpty())
                    System.out.println(EvidenceKt.getNativeJson().encodeToString(ActionEvidence.Companion.serializer(), action));
            }
        }
    }
}
