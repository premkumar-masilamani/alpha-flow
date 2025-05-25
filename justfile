setup:
    # Creating virtual environment...
    powershell -Command "pipenv shell"
    powershell -Command "pipenv install"

show:
    # Listing dependencies...
    powershell -Command "pipenv graph"

asta:
    powershell -Command "python src/main.py --config config/config.yaml"

renko:
    powershell -Command "python src/renko_chart.py --data-dir data --ticker BTC-USD --timeframe 1d"
