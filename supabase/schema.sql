-- =============================================================================
-- WishDrop Production PostgreSQL Schema for Supabase
-- Save. Organize. Share. Gift Smarter.
-- Includes Row-Level Security (RLS), Secret Gifts protection, & Realtime Sync
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. USERS
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_uid UUID UNIQUE,
    email TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    avatar_key TEXT NOT NULL DEFAULT 'avatar_attila',
    linked_member_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. FAMILY MEMBERS
CREATE TABLE IF NOT EXISTS public.family_members (
    id TEXT PRIMARY KEY,
    owner_user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT,
    relationship TEXT NOT NULL,
    avatar_key TEXT NOT NULL,
    accent_key TEXT NOT NULL DEFAULT 'indigo',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. SHARED LISTS (Supports PRIVATE, SHARED, HIDDEN, OWNER_ONLY + Secret Gifts)
CREATE TABLE IF NOT EXISTS public.shared_lists (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    emoji TEXT NOT NULL DEFAULT '🎁',
    subtitle TEXT NOT NULL DEFAULT '',
    owner_member_id TEXT NOT NULL REFERENCES public.family_members(id) ON DELETE CASCADE,
    visibility TEXT NOT NULL CHECK (visibility IN ('PRIVATE', 'SHARED', 'HIDDEN', 'OWNER_ONLY')),
    hidden_from_member_id TEXT REFERENCES public.family_members(id) ON DELETE SET NULL,
    event_date_label TEXT NOT NULL DEFAULT '',
    updated_at_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. LIST MEMBERS (Roles: OWNER, EDITOR, VIEWER, HIDDEN_GIFT_MANAGER)
CREATE TABLE IF NOT EXISTS public.list_members (
    list_id TEXT NOT NULL REFERENCES public.shared_lists(id) ON DELETE CASCADE,
    member_id TEXT NOT NULL REFERENCES public.family_members(id) ON DELETE CASCADE,
    member_name TEXT NOT NULL,
    member_email TEXT NOT NULL DEFAULT '',
    avatar_key TEXT NOT NULL DEFAULT 'avatar_attila',
    accent_key TEXT NOT NULL DEFAULT 'indigo',
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER', 'HIDDEN_GIFT_MANAGER')),
    joined_at_ms BIGINT NOT NULL,
    PRIMARY KEY (list_id, member_id)
);

-- 5. LIST INVITATIONS (Email Invite -> Accept -> Join List)
CREATE TABLE IF NOT EXISTS public.list_invitations (
    id TEXT PRIMARY KEY,
    list_id TEXT NOT NULL REFERENCES public.shared_lists(id) ON DELETE CASCADE,
    list_title TEXT NOT NULL,
    inviter_name TEXT NOT NULL,
    invitee_email TEXT NOT NULL,
    proposed_role TEXT NOT NULL CHECK (proposed_role IN ('OWNER', 'EDITOR', 'VIEWER', 'HIDDEN_GIFT_MANAGER')),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')) DEFAULT 'PENDING',
    created_at_ms BIGINT NOT NULL
);

-- 6. CATEGORIES
CREATE TABLE IF NOT EXISTS public.categories (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    icon_key TEXT NOT NULL,
    color_index INT NOT NULL DEFAULT 0
);

-- 7. TAGS
CREATE TABLE IF NOT EXISTS public.tags (
    name TEXT PRIMARY KEY,
    usage_count INT NOT NULL DEFAULT 1
);

-- 8. GIFT RECIPIENTS
CREATE TABLE IF NOT EXISTS public.gift_recipients (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    relationship TEXT NOT NULL,
    avatar_key TEXT NOT NULL,
    upcoming_event_name TEXT NOT NULL,
    days_until_event INT NOT NULL,
    budget_huf INT NOT NULL,
    interests_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    gift_stage_name TEXT NOT NULL DEFAULT 'IDEA',
    accent_key TEXT NOT NULL DEFAULT 'indigo'
);

-- 9. PRODUCTS
CREATE TABLE IF NOT EXISTS public.products (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    store TEXT NOT NULL,
    current_price_huf INT NOT NULL,
    original_price_huf INT NOT NULL,
    price_change_percent INT NOT NULL DEFAULT 0,
    savings_huf INT NOT NULL DEFAULT 0,
    image_key TEXT NOT NULL,
    source_url TEXT NOT NULL DEFAULT '',
    barcode TEXT NOT NULL DEFAULT '',
    categories_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    recipient_id TEXT REFERENCES public.gift_recipients(id) ON DELETE SET NULL,
    recipient_name TEXT,
    recipient_confidence INT NOT NULL DEFAULT 0,
    wishlist_id TEXT NOT NULL DEFAULT 'wl_personal',
    shared_list_id TEXT REFERENCES public.shared_lists(id) ON DELETE SET NULL,
    hidden_from_member_id TEXT REFERENCES public.family_members(id) ON DELETE SET NULL,
    collection_name TEXT NOT NULL DEFAULT 'Personal',
    is_purchased BOOLEAN NOT NULL DEFAULT FALSE,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_price_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured_deal BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NOT NULL DEFAULT '',
    created_at_ms BIGINT NOT NULL,
    alternatives_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    store_offers_json JSONB NOT NULL DEFAULT '[]'::jsonb
);

-- 10. PRODUCT_TAGS (Normalized Many-to-Many Join)
CREATE TABLE IF NOT EXISTS public.product_tags (
    product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    tag_name TEXT NOT NULL REFERENCES public.tags(name) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_name)
);

-- 11. COMMENTS (Product & Shared List Discussion)
CREATE TABLE IF NOT EXISTS public.comments (
    id TEXT PRIMARY KEY,
    product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    shared_list_id TEXT REFERENCES public.shared_lists(id) ON DELETE CASCADE,
    author_member_id TEXT NOT NULL,
    author_name TEXT NOT NULL,
    author_accent_key TEXT NOT NULL DEFAULT 'indigo',
    message TEXT NOT NULL,
    created_at_ms BIGINT NOT NULL
);

-- 12. ACTIVITY EVENTS (Audit & Realtime Timeline)
CREATE TABLE IF NOT EXISTS public.activity_events (
    id TEXT PRIMARY KEY,
    product_id TEXT REFERENCES public.products(id) ON DELETE CASCADE,
    shared_list_id TEXT REFERENCES public.shared_lists(id) ON DELETE CASCADE,
    actor_member_id TEXT NOT NULL,
    actor_name TEXT NOT NULL,
    actor_accent_key TEXT NOT NULL DEFAULT 'indigo',
    action_summary TEXT NOT NULL,
    event_type TEXT NOT NULL DEFAULT 'ITEM_UPDATED',
    created_at_ms BIGINT NOT NULL
);

-- 13. RECOMMENDATIONS (AI Gift Recommendations)
CREATE TABLE IF NOT EXISTS public.recommendations (
    id TEXT PRIMARY KEY,
    recipient_id TEXT NOT NULL REFERENCES public.gift_recipients(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    store TEXT NOT NULL,
    price_huf INT NOT NULL,
    image_key TEXT NOT NULL,
    match_percentage INT NOT NULL,
    reason TEXT NOT NULL,
    categories_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    tags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    linked_product_id TEXT REFERENCES public.products(id) ON DELETE SET NULL
);

-- 14. PRICE HISTORY
CREATE TABLE IF NOT EXISTS public.price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id TEXT NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    timestamp_ms BIGINT NOT NULL,
    price_huf INT NOT NULL,
    store_name TEXT NOT NULL
);

-- =============================================================================
-- INDEXES
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_products_shared_list ON public.products(shared_list_id);
CREATE INDEX IF NOT EXISTS idx_products_recipient ON public.products(recipient_id);
CREATE INDEX IF NOT EXISTS idx_comments_product ON public.comments(product_id, created_at_ms DESC);
CREATE INDEX IF NOT EXISTS idx_activity_product ON public.activity_events(product_id, created_at_ms DESC);
CREATE INDEX IF NOT EXISTS idx_activity_list ON public.activity_events(shared_list_id, created_at_ms DESC);
CREATE INDEX IF NOT EXISTS idx_price_history_product ON public.price_history(product_id, timestamp_ms ASC);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) — Enforces Secret Gifts Mode
-- =============================================================================
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.family_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.list_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.list_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.activity_events ENABLE ROW LEVEL SECURITY;

-- Helper resolving current authenticated user's family member ID
CREATE OR REPLACE FUNCTION public.current_family_member_id()
RETURNS TEXT
LANGUAGE sql
STABLE
AS $$
  SELECT linked_member_id FROM public.users WHERE auth_uid = auth.uid() LIMIT 1;
$$;

-- Secret Gifts Policy: A viewer NEVER sees a list where they are hidden_from_member_id,
-- and OWNER_ONLY lists are visible strictly to owner_member_id.
CREATE POLICY "shared_lists_secret_gift_select"
ON public.shared_lists
FOR SELECT
USING (
    (hidden_from_member_id IS NULL OR hidden_from_member_id <> COALESCE(public.current_family_member_id(), ''))
    AND (
        owner_member_id = public.current_family_member_id()
        OR (
            visibility <> 'OWNER_ONLY'
            AND EXISTS (
                SELECT 1 FROM public.list_members lm
                WHERE lm.list_id = public.shared_lists.id
                  AND lm.member_id = public.current_family_member_id()
            )
        )
    )
);

-- Products Policy: Hide secret gift items from the surprise recipient
CREATE POLICY "products_secret_gift_select"
ON public.products
FOR SELECT
USING (
    hidden_from_member_id IS NULL
    OR hidden_from_member_id <> COALESCE(public.current_family_member_id(), '')
);

-- Comments & Activity Policies: Respect parent product / shared list visibility
CREATE POLICY "comments_visible_if_product_visible"
ON public.comments
FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM public.products p
        WHERE p.id = public.comments.product_id
          AND (p.hidden_from_member_id IS NULL OR p.hidden_from_member_id <> COALESCE(public.current_family_member_id(), ''))
    )
);

CREATE POLICY "activity_visible_if_list_not_hidden"
ON public.activity_events
FOR SELECT
USING (
    shared_list_id IS NULL
    OR EXISTS (
        SELECT 1 FROM public.shared_lists sl
        WHERE sl.id = public.activity_events.shared_list_id
          AND (sl.hidden_from_member_id IS NULL OR sl.hidden_from_member_id <> COALESCE(public.current_family_member_id(), ''))
    )
);

-- =============================================================================
-- SUPABASE REALTIME PUBLICATION
-- =============================================================================
ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_lists;
ALTER PUBLICATION supabase_realtime ADD TABLE public.list_members;
ALTER PUBLICATION supabase_realtime ADD TABLE public.list_invitations;
ALTER PUBLICATION supabase_realtime ADD TABLE public.products;
ALTER PUBLICATION supabase_realtime ADD TABLE public.comments;
ALTER PUBLICATION supabase_realtime ADD TABLE public.activity_events;
