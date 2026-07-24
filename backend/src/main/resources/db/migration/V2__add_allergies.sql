ALTER TABLE app_user ADD COLUMN allergies VARCHAR(1000);

COMMENT ON COLUMN app_user.allergies IS
  'Free-text allergy/contraindication notes. Not returned by /api/users/by-role; '
  'only endpoints that need it for clinical decisions (e.g. prescribing) should read it.';
