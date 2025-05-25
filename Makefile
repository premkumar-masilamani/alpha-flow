.PHONY: setup
setup:
	@echo "Creating virtual environment..."
	pipenv shell

.PHONY: show
show:
	@echo "Listing dependencies..."
	pipenv graph


.PHONY: asta
asta:
	python3 src/main.py --config config/config.yaml

.PHONY: renko
renko:
	python3 src/renko_chart.py --data-dir data --ticker BTC-USD --timeframe 1d
