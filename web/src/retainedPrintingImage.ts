import type {CardImage} from './api';

export function retainedPrintingImage(source: {
  name: string; printingId: string; oracleId: string; set: string; collectorNumber: string;
  cardUrl: string; image: string | null; artist: string | null; sha256: string; retrievedAt?: string;
}): CardImage {
  return {
    index: 0, card: source.name, status: source.image ? 'VERIFIED' : 'UNAVAILABLE',
    reason: 'No front image in the retained printing snapshot.',
    printingId: source.printingId, oracleId: source.oracleId, set: source.set, collectorNumber: source.collectorNumber,
    fetchedAt: source.retrievedAt ?? null, sourceUrl: source.cardUrl, metadataSha256: source.sha256,
    algorithm: 'retained-printing-snapshot',
    faces: source.image ? [{name: source.name, url: source.image, artist: source.artist}] : [],
  };
}
