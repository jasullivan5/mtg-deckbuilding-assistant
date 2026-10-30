import {useState} from 'react';
import type {CardImage} from './api';

export function CardPicture({image, compact = false}: {image?: CardImage; compact?: boolean}) {
  const [face, setFace] = useState(0);
  const [failed, setFailed] = useState<string[]>([]);
  const current = image?.faces[face] ?? image?.faces[0];
  const available = image?.status === 'VERIFIED' && current && !failed.includes(current.url);
  return <figure className={compact ? 'picture thumbnail' : 'picture'}>
    {available ? <img src={current.url} alt={`${current.name} — ${image.set?.toUpperCase()} ${image.collectorNumber}`}
      loading="lazy" decoding="async" referrerPolicy="no-referrer" onError={() => setFailed(old => [...old, current.url])}/> :
      <div className="image-fallback" title={image?.reason}>
        <span aria-hidden="true">◇</span><small>{!image ? 'Checking image…' : 'Image unavailable'}</small>
      </div>}
    {!compact && <figcaption>
      {available ? <>
        {image.faces.length > 1 && <div className="face-buttons" aria-label="Card faces">
          {image.faces.map((f, i) => <button key={f.name} type="button" aria-pressed={face === i}
            onClick={() => setFace(i)}>{f.name}</button>)}
        </div>}
        <a href={image.sourceUrl!} target="_blank" rel="noreferrer">Scryfall · {image.set?.toUpperCase()} #{image.collectorNumber}</a>
        <span>Art: {current.artist ?? 'see card credit'} · © Wizards of the Coast</span>
      </> : <span>{image?.reason ?? 'Checking the printing and complete face identities.'}{image?.status === 'VERIFIED' && ' The verified image could not be loaded.'}</span>}
    </figcaption>}
  </figure>;
}
