/**
 * Frontend delivery charge estimation constants and helper.
 * Mirroring backend DeliveryChargeConfig for customer preview.
 * NOTE: The backend recalculates and binds the authoritative charge during checkout.
 */

export const BASE_CHARGE = 30.00;

export const WEIGHT_SURCHARGES = {
  LIGHT: 0.00,
  MEDIUM: 15.00,
  HEAVY: 30.00,
};

export const SIZE_SURCHARGES = {
  SMALL: 0.00,
  MEDIUM: 10.00,
  LARGE: 25.00,
};

export const SENSITIVE_SURCHARGE = 20.00;
export const PER_EXTRA_UNIT = 10.00;

/**
 * Calculates estimated delivery charge for a single catalog product line.
 * @param {Object} product
 * @param {number} quantity
 * @returns {number} estimated delivery fee in BDT
 */
export function calculateEstimatedDeliveryCharge(product, quantity = 1) {
  if (!product) return BASE_CHARGE;

  let charge = BASE_CHARGE;

  const weightFee = WEIGHT_SURCHARGES[product.weightClass] ?? 0;
  charge += weightFee;

  const sizeFee = SIZE_SURCHARGES[product.sizeClass] ?? 0;
  charge += sizeFee;

  if (product.isSensitive) {
    charge += SENSITIVE_SURCHARGE;
  }

  const qty = parseInt(quantity, 10);
  if (qty > 1) {
    charge += (qty - 1) * PER_EXTRA_UNIT;
  }

  return charge;
}
