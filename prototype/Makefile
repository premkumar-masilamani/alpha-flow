.PHONY: setup
setup:
	@echo "Creating virtual environment..."
	pipenv shell

.PHONY: show
show:
	@echo "Listing dependencies..."
	pipenv graph

.PHONY: clean
clean:
	@echo "Removing all pip environments..."
	PIPENV_IGNORE_VIRTUALENVS=1 pipenv --rm

.PHONY: run
run:
	make asta
	make renko

.PHONY: asta
asta:
	python3 src/main.py --config config/config.json

.PHONY: renko
renko:
	python3 src/renko_chart.py --data-dir data --ticker BTC-USD --timeframe 1d

.PHONY: tick daily renko plot clean

# -------- CONFIG --------
DATA_SRC = /Volumes/Prem/Binance/BTCUSDT
DATA_DIR = ./data
SYMBOL = BTCUSDT
TF = 1d

DAILY_FILE = $(DATA_DIR)/$(SYMBOL)-$(TF).csv
RENKO_FILE = $(DATA_DIR)/$(SYMBOL)-$(TF)-renko.csv


# -------- PIPELINE --------
tick: daily renko plot

daily:
	@echo "⏳ Generating daily OHLCV + order-flow..."
	python3 tick/tick_to_daily.py $(DATA_SRC) $(DAILY_FILE)

renko:
	@echo "🧱 Building Renko control chart..."
	python3 tick/daily_to_renko.py $(DAILY_FILE) $(RENKO_FILE)

plot:
	@echo "📈 Plotting Renko..."
	python3 tick/plot_renko.py --renko-file-path $(RENKO_FILE)
