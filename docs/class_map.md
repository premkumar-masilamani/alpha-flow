# AlphaFlow Class Map & Package Dependency Report

This report was generated dynamically by parsing the backend codebase. It provides a visual class dependency map and flags any violations of package separation rules.

## ✅ Package Separation Status

> [!NOTE]
> All classes adhere to the clean separation boundaries. No violations detected.

## Class Dependency Diagram (Mermaid)

```mermaid
flowchart TD
    subgraph api ["API Layer"]
        com_alphaflow_api_config_TimeframeConverter["TimeframeConverter (class)"]
        com_alphaflow_api_dtos_OhlcvDTO["OhlcvDTO (record)"]
        com_alphaflow_api_dtos_TickerDTO["TickerDTO (record)"]
        com_alphaflow_api_dtos_IndicatorSeriesDTO["IndicatorSeriesDTO (record)"]
        com_alphaflow_api_dtos_IndicatorConfigDTO["IndicatorConfigDTO (record)"]
        com_alphaflow_api_dtos_IndicatorPointDTO["IndicatorPointDTO (record)"]
        com_alphaflow_api_configs_CORSConfig["CORSConfig (class)"]
        com_alphaflow_api_mappers_IndicatorMapper["IndicatorMapper (class)"]
        com_alphaflow_api_mappers_OhlcvMapper["OhlcvMapper (class)"]
        com_alphaflow_api_mappers_TickerMapper["TickerMapper (class)"]
        com_alphaflow_api_controllers_TickerController["TickerController (class)"]
        com_alphaflow_api_controllers_PriceController["PriceController (class)"]
        com_alphaflow_api_controllers_IndicatorController["IndicatorController (class)"]
        com_alphaflow_api_controllers_generic_GlobalExceptionHandler["GlobalExceptionHandler (class)"]
        com_alphaflow_api_controllers_generic_ApiController["ApiController (class)"]
        com_alphaflow_api_services_DailyPriceService["DailyPriceService (class)"]
        com_alphaflow_api_services_TickerService["TickerService (class)"]
        com_alphaflow_api_services_WeeklyPriceService["WeeklyPriceService (class)"]
        com_alphaflow_api_services_IndicatorService["IndicatorService (class)"]
    end

    subgraph engine ["Engine (Logic)"]
        com_alphaflow_engine_indicators_SMAIndicator["SMAIndicator (class)"]
        com_alphaflow_engine_indicators_Indicator["Indicator (interface)"]
        com_alphaflow_engine_indicators_RSIIndicator["RSIIndicator (class)"]
        com_alphaflow_engine_indicators_MACDIndicator["MACDIndicator (class)"]
        com_alphaflow_engine_indicators_StochasticIndicator["StochasticIndicator (class)"]
        com_alphaflow_engine_indicators_EMAIndicator["EMAIndicator (class)"]
        com_alphaflow_engine_indicators_utils_EMAAccumulator["EMAAccumulator (class)"]
        com_alphaflow_engine_indicators_utils_IndicatorMath["IndicatorMath (class)"]
        com_alphaflow_engine_indicators_utils_IndicatorRegistry["IndicatorRegistry (class)"]
        com_alphaflow_engine_indicators_dtos_IndicatorParams["IndicatorParams (class)"]
        com_alphaflow_engine_indicators_dtos_PriceBar["PriceBar (record)"]
        com_alphaflow_engine_downloaders_YahooFinanceDownloader["YahooFinanceDownloader (class)"]
        com_alphaflow_engine_downloaders_YahooResponseParser["YahooResponseParser (class)"]
        com_alphaflow_engine_configs_YahooFinanceConfig["YahooFinanceConfig (class)"]
        com_alphaflow_engine_configs_IndicatorConfig["IndicatorConfig (class)"]
        com_alphaflow_engine_calculators_WeeklyPriceCalculator["WeeklyPriceCalculator (class)"]
        com_alphaflow_engine_calculators_IndicatorCalculator["IndicatorCalculator (class)"]
        com_alphaflow_engine_schedulers_CoreScheduler["CoreScheduler (class)"]
    end

    subgraph persistence ["Persistence (Database)"]
        com_alphaflow_persistence_enums_IndicatorOutputKey["IndicatorOutputKey (enum)"]
        com_alphaflow_persistence_enums_IndicatorParamKey["IndicatorParamKey (enum)"]
        com_alphaflow_persistence_enums_IndicatorType["IndicatorType (enum)"]
        com_alphaflow_persistence_enums_PriceSource["PriceSource (enum)"]
        com_alphaflow_persistence_repositories_TickerRepository["TickerRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyPriceRepository["WeeklyPriceRepository (interface)"]
        com_alphaflow_persistence_repositories_IndicatorDefinitionRepository["IndicatorDefinitionRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyIndicatorRepository["WeeklyIndicatorRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyIndicatorRepository["DailyIndicatorRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyPriceRepository["DailyPriceRepository (interface)"]
        com_alphaflow_persistence_exceptions_ResourceNotFoundException["ResourceNotFoundException (class)"]
        com_alphaflow_persistence_entities_DailyIndicator["DailyIndicator (class)"]
        com_alphaflow_persistence_entities_WeeklyIndicator["WeeklyIndicator (class)"]
        com_alphaflow_persistence_entities_IndicatorDefinition["IndicatorDefinition (class)"]
        com_alphaflow_persistence_entities_DailyPrice["DailyPrice (class)"]
        com_alphaflow_persistence_entities_Indicator["Indicator (class)"]
        com_alphaflow_persistence_entities_WeeklyPrice["WeeklyPrice (class)"]
        com_alphaflow_persistence_entities_Ticker["Ticker (class)"]
    end

    com_alphaflow_AlphaFlowApp["AlphaFlowApp (Root)"]
    com_alphaflow_common_enums_Timeframe["Timeframe (Root)"]

    %% Edges
    com_alphaflow_api_config_TimeframeConverter --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_dtos_IndicatorConfigDTO
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_dtos_IndicatorSeriesDTO
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_services_IndicatorService
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_DailyPriceService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_WeeklyPriceService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_controllers_TickerController --> com_alphaflow_api_dtos_TickerDTO
    com_alphaflow_api_controllers_TickerController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_generic_GlobalExceptionHandler --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_dtos_IndicatorPointDTO --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_api_dtos_IndicatorSeriesDTO --> com_alphaflow_api_dtos_IndicatorPointDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorConfigDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorPointDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorSeriesDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_api_mappers_TickerMapper --> com_alphaflow_api_dtos_TickerDTO
    com_alphaflow_api_mappers_TickerMapper --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_api_mappers_OhlcvMapper
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_dtos_IndicatorConfigDTO
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_dtos_IndicatorSeriesDTO
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_mappers_IndicatorMapper
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_DailyIndicatorRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_IndicatorDefinitionRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_WeeklyIndicatorRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_api_services_TickerService --> com_alphaflow_api_dtos_TickerDTO
    com_alphaflow_api_services_TickerService --> com_alphaflow_api_mappers_TickerMapper
    com_alphaflow_api_services_TickerService --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_services_TickerService --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_api_mappers_OhlcvMapper
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_utils_IndicatorRegistry
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_DailyIndicatorRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_IndicatorDefinitionRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_WeeklyIndicatorRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_persistence_repositories_IndicatorDefinitionRepository
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_engine_configs_YahooFinanceConfig
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_engine_downloaders_YahooResponseParser
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_engine_indicators_utils_EMAAccumulator
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_EMAIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_engine_indicators_utils_IndicatorRegistry
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_engine_indicators_utils_EMAAccumulator
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_MACDIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_RSIIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_SMAIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_StochasticIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_dtos_IndicatorParams --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_dtos_PriceBar --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_utils_EMAAccumulator --> com_alphaflow_engine_indicators_EMAIndicator
    com_alphaflow_engine_indicators_utils_EMAAccumulator --> com_alphaflow_engine_indicators_MACDIndicator
    com_alphaflow_engine_indicators_utils_EMAAccumulator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_utils_IndicatorRegistry --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_utils_IndicatorRegistry --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_IndicatorCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_WeeklyPriceCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_downloaders_YahooFinanceDownloader
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_DailyPrice --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_persistence_entities_IndicatorDefinition --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_persistence_entities_IndicatorDefinition --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_persistence_entities_WeeklyIndicator --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_persistence_entities_WeeklyIndicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_WeeklyIndicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_WeeklyPrice --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyPriceRepository --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_persistence_repositories_DailyPriceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_IndicatorDefinitionRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_TickerRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyIndicatorRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_WeeklyIndicatorRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyIndicatorRepository --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_persistence_repositories_WeeklyPriceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyPriceRepository --> com_alphaflow_persistence_entities_WeeklyPrice

    %% Styles for Violations
```

## Component Summary

- **Total Classes**: 57
- **Total Dependencies**: 178
- **API Layer Classes**: 19
- **Engine Layer Classes**: 18
- **Persistence Layer Classes**: 18
