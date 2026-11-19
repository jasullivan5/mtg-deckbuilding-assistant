import dev.mtg.assistant.Catalog;
import dev.mtg.assistant.discovery.*;
import com.wingedsheep.sdk.model.CardDefinition;
import java.util.*;

public class AuraInventory {
    static boolean candidate(CardDefinition card) {
        if (card.isAura() || card.getScript().getAuraTarget() != null) return true;
        if (card.getBackFace() != null && candidate(card.getBackFace())) return true;
        if (card.getFlipSide() != null && candidate(card.getFlipSide())) return true;
        return card.getCardFaces().stream().anyMatch(face -> face.getTypeLine().isAura() || face.getScript().getAuraTarget() != null);
    }
    public static void main(String[] args) {
        Catalog catalog = new Catalog();
        var physical = EligibilityKt.physicalCards(catalog.getCards());
        int roots = 0, relations = 0, declarations = 0;
        Set<String> improved = new TreeSet<>();
        Map<String,Integer> counts = new TreeMap<>();
        Map<String,Integer> filters = new TreeMap<>();
        Set<String> aliases = new TreeSet<>();
        for (var card : physical) {
            if (card.getBackFace() != null) aliases.add(card.getBackFace().getName());
            if (card.getFlipSide() != null) aliases.add(card.getFlipSide().getName());
            if (!candidate(card)) continue;
            roots++;
            var extracted = new PathwayExtractor().extract(List.of(card));
            for (var action : extracted.getActions()) {
                var relation = action.getAttachment();
                if (relation == null) continue;
                relations++;
                counts.merge("restriction:" + relation.getSupport(),1,Integer::sum);
                if (relation.getSupport() == Verdict.MATCH)
                    filters.merge(relation.getRestriction().getDescription(),1,Integer::sum);
                for (var dependency : relation.getDependencies()) {
                    declarations++;
                    counts.merge(dependency.getKind() + ":" + dependency.getVerdict(),1,Integer::sum);
                    if (dependency.getVerdict() == Verdict.MATCH) improved.add(card.getName());
                }
            }
        }
        System.out.println("physicalAuraBearingCards=" + roots);
        System.out.println("attachmentRecords=" + relations);
        System.out.println("declarations=" + declarations);
        System.out.println("improvedPhysicalCards=" + improved.size());
        counts.forEach((key,value) -> System.out.println(key + "=" + value));
        filters.forEach((key,value) -> System.out.println("filter:" + key + "=" + value));
        catalog.getCards().stream().filter(card -> aliases.contains(card.getName()) && candidate(card))
            .forEach(card -> System.out.println("registryAuraAlias=" + card.getName()));
    }
}
