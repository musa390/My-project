/**
 * LUXE PERFUME public-facing demo configuration.
 * Replace these demo values with verified business details before launch.
 * Keep secrets (email API keys, passwords) in .env.local, never in this file.
 */
export const siteContact = {
  announcement: "WELCOME TO LUXE PERFUME · A LITTLE LUXURY, EVERY DAY",
  businessName: "LUXE PERFUME",
  businessAddress: "",
  phone: "",
  supportEmail: "",
  whatsapp: "",
  whatsappButtonText: "Chat with us",
  businessHours: "Demo hours: Saturday–Thursday, 10:00 AM–6:00 PM (replace before launch)",
  social: {
    facebook: "",
    instagram: "",
    tiktok: "",
    youtube: "",
  },
  googleMapsEmbedUrl: "",
} as const;

/** Set NEXT_PUBLIC_CONTACT_FORM_MODE=demo or email in .env.local. */
export const contactFormMode =
  process.env.NEXT_PUBLIC_CONTACT_FORM_MODE === "email" ? "email" : "demo";
