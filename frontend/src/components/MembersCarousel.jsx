import { useCallback } from 'react';
import useEmblaCarousel from 'embla-carousel-react';
import Autoplay from 'embla-carousel-autoplay';
import members from '../data/members.json';

const PLACEHOLDER = '/members/placeholder.svg';

/**
 * Auto-scrolling, swipeable committee carousel. Reads from data/members.json and
 * loads photos from /public/members/<file>. Falls back to a neutral placeholder
 * until the real photos are dropped in. Autoplay pauses on hover/interaction.
 */
export default function MembersCarousel() {
  const [emblaRef] = useEmblaCarousel(
    { loop: true, align: 'start', dragFree: true },
    [Autoplay({ delay: 2200, stopOnInteraction: false, stopOnMouseEnter: true })]
  );

  const onImgError = useCallback((e) => {
    if (e.currentTarget.src.endsWith(PLACEHOLDER)) return;
    e.currentTarget.src = PLACEHOLDER;
  }, []);

  return (
    <div className="embla" ref={emblaRef} aria-label="Committee members" tabIndex={0}>
      <div className="embla__container">
        {members.map((m, i) => (
          <div className="embla__slide" key={`${m.name}-${i}`}>
            <article className="member-card">
              <img
                className="photo"
                src={m.photo}
                alt={`${m.name}, ${m.designation}`}
                loading="lazy"
                onError={onImgError}
              />
              <div className="info">
                <div className="name">{m.name}</div>
                <div className="desig badge green">{m.designation}</div>
              </div>
            </article>
          </div>
        ))}
      </div>
    </div>
  );
}
