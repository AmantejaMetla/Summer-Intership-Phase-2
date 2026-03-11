import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { apiJson } from '../api/client'
import './Home.css'

/** Hero: 3 rotating coffees. Put these in frontend/public/images/ */
const HERO_SHOWCASE_IMAGES = [
  '/images/homepage1-removebg-preview.png',
  '/images/homepage2-removebg-preview.png',
  '/images/Homepage3-removebg-preview.png',
]
const HERO_ROTATE_INTERVAL_MS = 5000

/** Per-slide title and subtext (one per homepage image) */
const HERO_SLIDE_TEXTS = [
  { title: 'Rich espresso & cappuccino', tagline: 'Freshly brewed, smooth and bold.' },
  { title: 'Iced drinks & cold brews', tagline: 'Cool sips for any moment.' },
  { title: 'Snacks & something sweet', tagline: 'Pair your drink with a bite.' },
]

/** Sample image and set CSS vars for hero + page background tint (lightened to stay readable) */
function useImageBackground(imageUrl) {
  useEffect(() => {
    if (!imageUrl) {
      document.body.style.removeProperty('--bg-hero')
      document.body.style.removeProperty('--bg-page')
      return
    }
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        const size = 40
        canvas.width = size
        canvas.height = size
        const ctx = canvas.getContext('2d')
        ctx.drawImage(img, 0, 0, size, size)
        const data = ctx.getImageData(0, 0, size, size).data
        let r = 0, g = 0, b = 0, n = 0
        for (let i = 0; i < data.length; i += 4) {
          r += data[i]
          g += data[i + 1]
          b += data[i + 2]
          n++
        }
        if (n > 0) {
          r = Math.round(r / n)
          g = Math.round(g / n)
          b = Math.round(b / n)
          const mix = (x, white = 0.92) => Math.round(x * (1 - white) + 255 * white)
          const hr = mix(r), hg = mix(g), hb = mix(b)
          const pr = mix(r, 0.97), pg = mix(g, 0.97), pb = mix(b, 0.97)
          document.body.style.setProperty('--bg-hero', `rgb(${hr},${hg},${hb})`)
          document.body.style.setProperty('--bg-page', `rgb(${pr},${pg},${pb})`)
        }
      } catch (_) {
        document.body.style.removeProperty('--bg-hero')
        document.body.style.removeProperty('--bg-page')
      }
    }
    img.onerror = () => {
      document.body.style.removeProperty('--bg-hero')
      document.body.style.removeProperty('--bg-page')
    }
    img.src = imageUrl
    return () => {
      document.body.style.removeProperty('--bg-hero')
      document.body.style.removeProperty('--bg-page')
    }
  }, [imageUrl])
}

export function Home() {
  const [heroProducts, setHeroProducts] = useState([])
  const [promotion, setPromotion] = useState(null)
  const [heroSlideIndex, setHeroSlideIndex] = useState(0)
  const [apiError, setApiError] = useState(false)

  const showcaseImage = HERO_SHOWCASE_IMAGES[heroSlideIndex]
  useImageBackground(showcaseImage)

  const goToNextSlide = useCallback(() => {
    setHeroSlideIndex((i) => (i + 1) % HERO_SHOWCASE_IMAGES.length)
  }, [])

  const goToPrevSlide = useCallback(() => {
    setHeroSlideIndex((i) => (i - 1 + HERO_SHOWCASE_IMAGES.length) % HERO_SHOWCASE_IMAGES.length)
  }, [])

  const goToSlide = useCallback((index) => {
    if (index !== heroSlideIndex) setHeroSlideIndex(index)
  }, [heroSlideIndex])

  useEffect(() => {
    const t = setInterval(goToNextSlide, HERO_ROTATE_INTERVAL_MS)
    return () => clearInterval(t)
  }, [goToNextSlide])

  useEffect(() => {
    let cancelled = false
    async function load() {
      setApiError(false)
      try {
        const [data, promo] = await Promise.all([
          apiJson('/api/products'),
          apiJson('/api/products/promotions/coffee-of-the-day').catch(() => null),
        ])
        const list = Array.isArray(data) ? data : []
        const featured = list.length > 6 ? list.slice(0, 6) : list
        if (!cancelled) {
          setHeroProducts(featured)
          setPromotion(promo)
        }
      } catch (_) {
        if (!cancelled) setApiError(true)
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  /* Center always shows current coffee; scattered beans only in background (same transition for every change) */
  const heroImageSrc = showcaseImage
  const heroImageAlt = `Coffee showcase ${heroSlideIndex + 1}`

  const slideText = HERO_SLIDE_TEXTS[heroSlideIndex] || HERO_SLIDE_TEXTS[0]
  const fullTitle = slideText.title
  const [typedTitle, setTypedTitle] = useState('')
  const isTyping = typedTitle.length < fullTitle.length

  useEffect(() => {
    const full = HERO_SLIDE_TEXTS[heroSlideIndex]?.title ?? ''
    setTypedTitle('')
    if (!full) return
    let i = 0
    const id = setInterval(() => {
      i += 1
      setTypedTitle(full.slice(0, i))
      if (i >= full.length) clearInterval(id)
    }, 50)
    return () => clearInterval(id)
  }, [heroSlideIndex])

  return (
    <>
      <section className="hero">
        {apiError && (
          <div className="hero-api-error" role="alert">
            Can't reach the server. Start the backend (Eureka, Gateway, and services). See <strong>SETUP-ON-ANOTHER-LAPTOP.md</strong> or <strong>README.md</strong>.
          </div>
        )}
        <div className="hero-inner">
          <div className="hero-image-col">
            <div className="hero-product">
              <img key={heroSlideIndex} src={heroImageSrc} alt={heroImageAlt} className="hero-product-image" />
              <button
                type="button"
                className="hero-arrow hero-arrow--left"
                onClick={goToPrevSlide}
                aria-label="Previous slide"
              >
                <span className="hero-arrow-icon">‹</span>
              </button>
              <button
                type="button"
                className="hero-arrow hero-arrow--right"
                onClick={goToNextSlide}
                aria-label="Next slide"
              >
                <span className="hero-arrow-icon">›</span>
              </button>
            </div>
            <div className="hero-dots" role="tablist" aria-label="Hero carousel">
              {HERO_SHOWCASE_IMAGES.map((_, i) => (
                <button
                  key={i}
                  type="button"
                  role="tab"
                  aria-selected={i === heroSlideIndex}
                  aria-label={`Slide ${i + 1}`}
                  className={`hero-dot ${i === heroSlideIndex ? 'hero-dot--active' : ''}`}
                  onClick={() => goToSlide(i)}
                />
              ))}
            </div>
          </div>
          <div className="hero-text-col" key={heroSlideIndex}>
            <h1 className="hero-title hero-title--animate">
              {typedTitle}
              {isTyping && <span className="hero-title-cursor" aria-hidden="true">|</span>}
            </h1>
            <p className="hero-tagline hero-tagline--animate">
              {slideText.tagline}
            </p>
            <Link to="/shop" className="btn-shop">
              Shop now
            </Link>
          </div>
        </div>
      </section>
      {heroProducts.length > 0 && (
        <section className="home-featured">
          {promotion?.productId ? (
            <div className="home-promo-banner">
              <strong>{promotion.couponCode}</strong> - {promotion.message}
            </div>
          ) : null}
          <h2 className="home-featured-title">Browse</h2>
          <div className="home-featured-grid">
            {heroProducts.slice(0, 4).map((p) => (
              <Link
                key={p.id}
                to={`/shop/${p.id}`}
                className={`home-featured-card ${promotion?.productId === p.id ? 'home-featured-card--promo' : ''}`}
              >
                {promotion?.productId === p.id ? (
                  <span className="home-featured-promo-badge">Coffee of the Day</span>
                ) : null}
                {p.imageUrl ? (
                  <img src={p.imageUrl} alt={p.name} />
                ) : (
                  <div className="home-featured-placeholder" />
                )}
                <span className="home-featured-name">{p.name}</span>
              </Link>
            ))}
          </div>
        </section>
      )}
    </>
  )
}
