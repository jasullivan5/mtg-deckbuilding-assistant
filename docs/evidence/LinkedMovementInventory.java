import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.*;
import java.util.*;

/** Streaming physical-card/face inventory using the production walker at the normal 1 GiB limit.
 * Raw output is generated evidence. The companion reducer counts exact serialized native nodes. */
public class LinkedMovementInventory {
    static String quote(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    public static void main(String[] args) {
        var catalog = new Catalog();
        for (var card : EligibilityKt.physicalCards(catalog.getCards())) {
            var x = new PathwayExtractor().extract(List.of(card));
            for (var root : x.getActions()) {
                if (!root.getPath().equals("script")) continue;
                var links = new ArrayList<String>();
                for (var link : x.getLinkedMovements()) if (link.getFace().equals(root.getFace()))
                    links.add(EvidenceKt.getReportJson().encodeToString(LinkedMovement.Companion.serializer(), link));
                System.out.println("{\"physical\":" + quote(card.getName()) + ",\"face\":" + quote(root.getFace()) +
                    ",\"script\":" + root.getNative() + ",\"links\":[" + String.join(",",links) + "]}");
            }
        }
    }
}
