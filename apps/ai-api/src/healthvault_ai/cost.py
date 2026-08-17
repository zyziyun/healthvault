"""Token, cost, and latency accounting.

Every model call has a price and a delay, and both scale with tokens. Even a rough estimate makes
those visible so they can be reasoned about before they show up on a bill or a p99 latency chart.
Prices are USD per 1M tokens (input, output); update them from the provider's pricing page.
"""

from __future__ import annotations

# USD per 1,000,000 tokens: (input, output). Illustrative values, verify against current pricing.
PRICES: dict[str, tuple[float, float]] = {
    "gpt-4o-mini": (0.15, 0.60),
    "gpt-4o": (2.50, 10.00),
}


def estimate_tokens(text: str) -> int:
    """Rough token count: ~4 characters per token for English. Real code uses the model's tokenizer."""
    return max(1, len(text) // 4)


def estimate_cost(model: str, input_tokens: int, output_tokens: int) -> float:
    in_price, out_price = PRICES.get(model, (0.0, 0.0))
    cost = (input_tokens * in_price + output_tokens * out_price) / 1_000_000
    return round(cost, 6)
