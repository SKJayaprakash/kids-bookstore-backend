-- Add slug and customDomain columns to shops table
ALTER TABLE shops ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
ALTER TABLE shops ADD COLUMN IF NOT EXISTS custom_domain VARCHAR(255);

-- Generate slugs from existing shop names for data migration
UPDATE shops 
SET slug = LOWER(REGEXP_REPLACE(REGEXP_REPLACE(name, '[^a-zA-Z0-9\\s-]', '', 'g'), '\\s+', '-', 'g'))
WHERE slug IS NULL;

-- Add unique constraints
ALTER TABLE shops ADD CONSTRAINT IF NOT EXISTS uk_shop_slug UNIQUE (slug);
ALTER TABLE shops ADD CONSTRAINT IF NOT EXISTS uk_shop_custom_domain UNIQUE (custom_domain);

-- Make slug NOT NULL after data migration
ALTER TABLE shops ALTER COLUMN slug SET NOT NULL;

-- Add index for faster lookups
CREATE INDEX IF NOT EXISTS idx_shops_slug ON shops(slug);
CREATE INDEX IF NOT EXISTS idx_shops_custom_domain ON shops(custom_domain);
