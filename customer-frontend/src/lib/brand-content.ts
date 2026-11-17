/**
 * Editable LUXE brand-story and campaign content.
 * Replace demo copy/media with approved brand information before launch.
 * For video, use a direct MP4/WebM URL that you own or are licensed to use.
 */
export const brandContent = {
  campaignImageUrl:
    "https://images.unsplash.com/photo-1615634260167-c8cdede054de?auto=format&fit=crop&w=1800&q=85",
  campaignImageAlt: "Luxury fragrance campaign editorial placeholder",
  aboutMedia: {
    type: "image" as "image" | "video",
    imageUrl:
      "https://images.unsplash.com/photo-1595535373192-fc8935bacd89?auto=format&fit=crop&w=1200&q=85",
    videoUrl: "",
    alt: "LUXE PERFUME brand editorial placeholder",
  },
  aboutStory: {
    heading: "Fragrance is the art of memory.",
    paragraphs: [
      "LUXE PERFUME is creating a thoughtful place to explore fragrance. This demo story is intentionally editable: add your real founding story, team details and brand history before launch.",
      "Our goal is to make fragrance discovery clear, thoughtful and personal.",
    ],
  },
  mission: {
    title: "Make discovery personal",
    description: "Help customers explore fragrance notes and choose a scent that suits their own preferences.",
  },
  vision: {
    title: "A thoughtful fragrance destination",
    description: "Build a clear shopping experience with transparent product and delivery information.",
  },
  values: {
    title: "Clarity, care & craft",
    description: "Publish accurate product details, respect customer information and communicate honestly.",
  },
  craftsmanship: "Use this section to describe your verified product selection, fragrance composition, packaging and quality standards. Avoid claims that cannot be substantiated.",
  sustainability: "Add confirmed packaging, sourcing and environmental commitments here. This demo makes no unverified sustainability claims.",
  timeline: [
    { title: "01 · THE IDEA", description: "Demo milestone — add the date and story behind the brand." },
    { title: "02 · THE COLLECTION", description: "Demo milestone — add the launch of your first collection." },
    { title: "03 · WHAT COMES NEXT", description: "Demo milestone — share your next verified brand milestone." },
  ],
  testimonials: [
    { slot: "REVIEW SLOT 01", quote: "Add an approved customer review here.", attribution: "Verified customer name · to be added" },
    { slot: "REVIEW SLOT 02", quote: "Add an approved customer review here.", attribution: "Verified customer name · to be added" },
    { slot: "REVIEW SLOT 03", quote: "Add an approved customer review here.", attribution: "Verified customer name · to be added" },
  ],
} as const;
