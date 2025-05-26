setup:
    # Creating virtual environment...
    pipenv shell
    pipenv install

show:
    # Listing dependencies...
    pipenv graph

run:
    clear
    just asta
    just renko

asta:
    python src/main.py --config config/config.yaml

renko:
    python src/renko_chart.py --data-dir data --ticker BTC-USD --timeframe 1d
