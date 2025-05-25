# Set global shell for all recipes to PowerShell
set shell := ["powershell", "-NoProfile", "-NoLogo", "-Command"]

venv_dir := ".venv"

default:
    just setup
    just install
    just activate

run:
    just asta
    just renko

setup:
    Write-Host "Creating virtual environment..."
    python -m venv {{venv_dir}}

install:
    Write-Host "Installing dependencies..."
    .\{{venv_dir}}\Scripts\pip.exe install -r requirements.txt

activate:
    Write-Host "Activating virtual environment..."
    Write-Host "Run the below command in PowerShell:"
    Write-Host ".\{{venv_dir}}\Scripts\Activate.ps1"

test:
    $env:PYTHONPATH = "src"
    .\{{venv_dir}}\Scripts\python.exe -m pytest tests/ --cov=src --cov-report=term-missing --cov-fail-under=30 --cov-report=html

asta:
    $env:PYTHONPATH = "src"
    .\{{venv_dir}}\Scripts\python.exe src/main.py --config config/config.yaml

sr:
    $env:PYTHONPATH = "src"
    .\{{venv_dir}}\Scripts\python.exe src/technical_analysis/sr_zones.py --data-dir data --ticker BTC-USD --timeframe 1d

renko:
    $env:PYTHONPATH = "src"
    .\{{venv_dir}}\Scripts\python.exe src/renko.py --data-dir data --ticker BTC-USD --timeframe 1d
