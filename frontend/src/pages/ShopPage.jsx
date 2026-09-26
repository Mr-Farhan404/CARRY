import React, { useState, useEffect, useMemo } from 'react';
import { productsApi } from '../services/api';
import { useCart } from '../context/CartContext';
import { calculateEstimatedDeliveryCharge } from '../constants/deliveryCharge';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import Card from '../components/common/Card';

export default function ShopPage() {
  const { addToCart, openCart, totalCount } = useCart();

  const [products, setProducts] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');

  // Local quantity selections per product card (default 1)
  const [quantities, setQuantities] = useState({});

  // Fetch all active products once on mount
  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    setError(null);

    productsApi.getActive()
      .then((data) => {
        if (isMounted) {
          setProducts(data || []);
        }
      })
      .catch((err) => {
        if (isMounted) {
          console.error('Failed to load active products:', err);
          setError(err.message || 'Failed to load catalog products');
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Filter products client-side by category and search keyword
  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      const matchesCategory =
        selectedCategory === 'ALL' || p.category === selectedCategory;

      const q = searchQuery.toLowerCase().trim();
      const matchesSearch =
        !q ||
        p.name?.toLowerCase().includes(q) ||
        p.description?.toLowerCase().includes(q);

      return matchesCategory && matchesSearch;
    });
  }, [products, selectedCategory, searchQuery]);

  const handleQuantityChange = (productId, delta) => {
    setQuantities((prev) => {
      const current = prev[productId] || 1;
      const next = Math.max(1, current + delta);
      return { ...prev, [productId]: next };
    });
  };

  const handleAddToCart = (product) => {
    const qty = quantities[product.id] || 1;
    addToCart(product, qty);
    // Reset local card quantity selector back to 1
    setQuantities((prev) => ({ ...prev, [product.id]: 1 }));
    openCart();
  };

  return (
    <div className="shop-page">
      {/* Hero Banner */}
      <div className="shop-hero">
        <div className="shop-hero__content">
          <div className="shop-hero__badge">Campus Catalog</div>
          <h1 className="shop-hero__title">Campus Store & Products</h1>
          <p className="shop-hero__subtitle">
            Browse verified food, electronics, and daily essentials. Order in one click and have traveling KUET students deliver directly to your campus hall!
          </p>
        </div>

        <div className="shop-hero__cart-btn-wrap">
          <Button variant="primary" onClick={openCart} className="shop-view-cart-btn">
            <span>🛒 Cart</span>
            {totalCount > 0 && <span className="shop-cart-pill">{totalCount}</span>}
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="shop-toolbar">
        <div className="shop-search-box">
          <Input
            name="shopSearch"
            placeholder="Search products by name (e.g. Biryani, Arduino, Keyboard)..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="shop-category-pills">
          {['ALL', 'FOOD', 'ELECTRONICS', 'OTHERS'].map((cat) => (
            <button
              key={cat}
              type="button"
              className={`shop-category-pill ${
                selectedCategory === cat ? 'shop-category-pill--active' : ''
              }`}
              onClick={() => setSelectedCategory(cat)}
            >
              {cat === 'ALL' ? 'All Items' : cat.charAt(0) + cat.slice(1).toLowerCase()}
              <span className="shop-category-count">
                {cat === 'ALL'
                  ? products.length
                  : products.filter((p) => p.category === cat).length}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="ui-alert ui-alert--error mb-4" role="alert">
          <strong>Error loading products:</strong> {error}
        </div>
      )}

      {/* Loading state */}
      {isLoading ? (
        <div className="ui-loading-card">
          <span className="ui-btn__spinner" aria-hidden="true"></span>
          <span>Loading campus catalog...</span>
        </div>
      ) : filteredProducts.length === 0 ? (
        <Card className="empty-state-card">
          <div className="empty-state">
            <span className="empty-state__icon">🔍</span>
            <h3>No products found</h3>
            <p>Try searching with another keyword or selecting "All Items".</p>
            {(searchQuery || selectedCategory !== 'ALL') && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setSearchQuery('');
                  setSelectedCategory('ALL');
                }}
              >
                Reset Filters
              </Button>
            )}
          </div>
        </Card>
      ) : (
        /* Product Grid */
        <div className="shop-grid">
          {filteredProducts.map((product) => {
            const cardQty = quantities[product.id] || 1;
            const singleEstDelivery = calculateEstimatedDeliveryCharge(product, 1);
            const totalEstDelivery = calculateEstimatedDeliveryCharge(product, cardQty);

            return (
              <div key={product.id} className="product-card">
                <div className="product-card__image-wrap">
                  <img
                    src={product.imageUrl || 'https://picsum.photos/seed/carry/400/300'}
                    alt={product.name}
                    className="product-card__image"
                    loading="lazy"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = 'https://picsum.photos/seed/placeholder/400/300';
                    }}
                  />
                  <div className="product-card__category-badge">
                    {product.category}
                  </div>
                  {product.isSensitive && (
                    <div className="product-card__sensitive-badge">
                      ⚠️ Fragile
                    </div>
                  )}
                </div>

                <div className="product-card__content">
                  <h3 className="product-card__name" title={product.name}>
                    {product.name}
                  </h3>

                  {product.description && (
                    <p className="product-card__desc">
                      {product.description}
                    </p>
                  )}

                  <div className="product-card__specs">
                    <span className="spec-tag">Weight: {product.weightClass}</span>
                    <span className="spec-tag">Size: {product.sizeClass}</span>
                  </div>

                  <div className="product-card__pricing">
                    <div className="product-price">
                      ৳{parseFloat(product.estimatedPrice).toFixed(2)}
                    </div>
                    <div className="product-delivery-estimate" title="Dynamic delivery fee calculated from weight, size, and sensitivity">
                      +৳{singleEstDelivery.toFixed(2)} est. delivery
                    </div>
                  </div>

                  {/* Quantity selector & Add to cart */}
                  <div className="product-card__actions">
                    <div className="card-qty-control">
                      <button
                        type="button"
                        className="card-qty-btn"
                        onClick={() => handleQuantityChange(product.id, -1)}
                        aria-label="Decrease quantity"
                      >
                        −
                      </button>
                      <span className="card-qty-num">{cardQty}</span>
                      <button
                        type="button"
                        className="card-qty-btn"
                        onClick={() => handleQuantityChange(product.id, 1)}
                        aria-label="Increase quantity"
                      >
                        +
                      </button>
                    </div>

                    <Button
                      variant="primary"
                      className="card-add-cart-btn"
                      onClick={() => handleAddToCart(product)}
                    >
                      Add to Cart {cardQty > 1 && `(${cardQty})`}
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
