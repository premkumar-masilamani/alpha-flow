# AlphaFlow Class Map & Package Dependency Report

This report was generated dynamically by parsing the backend codebase. It provides a visual class dependency map and flags any violations of package separation rules.

## ⚠️ Architectural Violations

> [!WARNING]
> Found **4** architectural violations where package boundaries have been breached.

| Source Class | Target Dependency | Reason | Source File |
| :--- | :--- | :--- | :--- |
| `ASTAStrategy` (com.alphaflow.engine.strategies.ASTAStrategy) | `ASTAResponseDTO` (com.alphaflow.api.dtos.ASTAResponseDTO) | Engine component cannot depend on API layer DTOs or Controllers | [`ASTAStrategy.java`](file:///Users/premkumar/Code/alpha-flow/backend/src/main/java/com/alphaflow/engine/strategies/ASTAStrategy.java#L1) |
| `Indicator` (com.alphaflow.persistence.entities.Indicator) | `IndicatorParams` (com.alphaflow.engine.indicators.dtos.IndicatorParams) | Persistence database entities and repositories cannot depend on Business Logic (Engine) | [`Indicator.java`](file:///Users/premkumar/Code/alpha-flow/backend/src/main/java/com/alphaflow/persistence/entities/Indicator.java#L1) |
| `DailyPriceRepository` (com.alphaflow.persistence.repositories.DailyPriceRepository) | `OhlcvDTO` (com.alphaflow.api.dtos.OhlcvDTO) | Persistence component cannot depend on API layer DTOs or Controllers | [`DailyPriceRepository.java`](file:///Users/premkumar/Code/alpha-flow/backend/src/main/java/com/alphaflow/persistence/repositories/DailyPriceRepository.java#L1) |
| `DailyPriceRepository` (com.alphaflow.persistence.repositories.DailyPriceRepository) | `OhlcvMapper` (com.alphaflow.api.mappers.OhlcvMapper) | Persistence component cannot depend on API layer DTOs or Controllers | [`DailyPriceRepository.java`](file:///Users/premkumar/Code/alpha-flow/backend/src/main/java/com/alphaflow/persistence/repositories/DailyPriceRepository.java#L1) |

---

## Class Dependency Diagram (Mermaid)

```mermaid
flowchart TD
    subgraph api ["API Layer"]
        com_alphaflow_api_utils_APIUtil["APIUtil (class)"]
        com_alphaflow_api_dtos_ASTAResponseDTO["ASTAResponseDTO (record)"]
        com_alphaflow_api_dtos_OhlcvDTO["OhlcvDTO (record)"]
        com_alphaflow_api_dtos_TickerDTO["TickerDTO (record)"]
        com_alphaflow_api_dtos_IndicatorSeriesDTO["IndicatorSeriesDTO (record)"]
        com_alphaflow_api_dtos_IndicatorConfigDTO["IndicatorConfigDTO (record)"]
        com_alphaflow_api_dtos_IndicatorPointDTO["IndicatorPointDTO (record)"]
        com_alphaflow_api_configs_CORSConfig["CORSConfig (class)"]
        com_alphaflow_api_mappers_IndicatorMapper["IndicatorMapper (class)"]
        com_alphaflow_api_mappers_OhlcvMapper["OhlcvMapper (class)"]
        com_alphaflow_api_mappers_TickerMapper["TickerMapper (class)"]
        com_alphaflow_api_controllers_WeeklyPriceController["WeeklyPriceController (class)"]
        com_alphaflow_api_controllers_TickerController["TickerController (class)"]
        com_alphaflow_api_controllers_DailyPriceController["DailyPriceController (class)"]
        com_alphaflow_api_controllers_IndicatorController["IndicatorController (class)"]
        com_alphaflow_api_controllers_AnalysisController["AnalysisController (class)"]
        com_alphaflow_api_controllers_generic_GlobalExceptionHandler["GlobalExceptionHandler (class)"]
        com_alphaflow_api_controllers_generic_ApiController["ApiController (class)"]
        com_alphaflow_api_services_TickerService["TickerService (class)"]
        com_alphaflow_api_services_WeeklyPriceService["WeeklyPriceService (class)"]
        com_alphaflow_api_services_IndicatorService["IndicatorService (class)"]
    end

    subgraph engine ["Engine (Logic)"]
        com_alphaflow_engine_strategies_ASTAStrategy["ASTAStrategy (class)"]
        com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator["DailyRsiEvaluator (class)"]
        com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator["DailyStochasticEvaluator (class)"]
        com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator["DailyVolumeEvaluator (class)"]
        com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext["ASTAEvaluationContext (record)"]
        com_alphaflow_engine_strategies_evaluators_TradeSignal["TradeSignal (record)"]
        com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator["WeeklyMacdEvaluator (class)"]
        com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator["DailyEmaEvaluator (class)"]
        com_alphaflow_engine_strategies_evaluators_ASTAEvaluator["ASTAEvaluator (interface)"]
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
        com_alphaflow_persistence_enums_EvaluatorMessage["EvaluatorMessage (enum)"]
        com_alphaflow_persistence_enums_IndicatorOutputKey["IndicatorOutputKey (enum)"]
        com_alphaflow_persistence_enums_IndicatorParamKey["IndicatorParamKey (enum)"]
        com_alphaflow_persistence_enums_IndicatorType["IndicatorType (enum)"]
        com_alphaflow_persistence_enums_PriceSource["PriceSource (enum)"]
        com_alphaflow_persistence_enums_Timeframe["Timeframe (enum)"]
        com_alphaflow_persistence_enums_TradeAction["TradeAction (enum)"]
        com_alphaflow_persistence_repositories_TickerRepository["TickerRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyPriceRepository["WeeklyPriceRepository (interface)"]
        com_alphaflow_persistence_repositories_ASTAResultsRepository["ASTAResultsRepository (interface)"]
        com_alphaflow_persistence_repositories_IndicatorDefinitionRepository["IndicatorDefinitionRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyIndicatorRepository["WeeklyIndicatorRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyIndicatorRepository["DailyIndicatorRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyPriceRepository["DailyPriceRepository (interface)"]
        com_alphaflow_persistence_exceptions_ResourceNotFoundException["ResourceNotFoundException (class)"]
        com_alphaflow_persistence_entities_DailyIndicator["DailyIndicator (class)"]
        com_alphaflow_persistence_entities_WeeklyIndicator["WeeklyIndicator (class)"]
        com_alphaflow_persistence_entities_IndicatorDefinition["IndicatorDefinition (class)"]
        com_alphaflow_persistence_entities_ASTAResults["ASTAResults (class)"]
        com_alphaflow_persistence_entities_DailyPrice["DailyPrice (class)"]
        com_alphaflow_persistence_entities_Indicator["Indicator (class)"]
        com_alphaflow_persistence_entities_WeeklyPrice["WeeklyPrice (class)"]
        com_alphaflow_persistence_entities_Ticker["Ticker (class)"]
    end

    com_alphaflow_AlphaFlowApp["AlphaFlowApp (Root)"]

    %% Edges
    com_alphaflow_api_controllers_AnalysisController --> com_alphaflow_api_dtos_ASTAResponseDTO
    com_alphaflow_api_controllers_AnalysisController --> com_alphaflow_engine_strategies_ASTAStrategy
    com_alphaflow_api_controllers_DailyPriceController --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_controllers_DailyPriceController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_DailyPriceController --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_controllers_DailyPriceController --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_api_controllers_DailyPriceController --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_dtos_IndicatorConfigDTO
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_dtos_IndicatorSeriesDTO
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_services_IndicatorService
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_utils_APIUtil
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_controllers_TickerController --> com_alphaflow_api_dtos_TickerDTO
    com_alphaflow_api_controllers_TickerController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_WeeklyPriceController --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_controllers_WeeklyPriceController --> com_alphaflow_api_services_WeeklyPriceService
    com_alphaflow_api_controllers_generic_GlobalExceptionHandler --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_dtos_ASTAResponseDTO --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_api_dtos_IndicatorPointDTO --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_api_dtos_IndicatorSeriesDTO --> com_alphaflow_api_dtos_IndicatorPointDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorConfigDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorPointDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorSeriesDTO
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_Timeframe
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_api_mappers_TickerMapper --> com_alphaflow_api_dtos_TickerDTO
    com_alphaflow_api_mappers_TickerMapper --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_dtos_IndicatorConfigDTO
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_dtos_IndicatorSeriesDTO
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_mappers_IndicatorMapper
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_enums_Timeframe
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_ASTAResultsRepository
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
    com_alphaflow_api_utils_APIUtil --> com_alphaflow_persistence_enums_Timeframe
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_utils_IndicatorRegistry
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_enums_Timeframe
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_ASTAResultsRepository
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
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_persistence_enums_Timeframe
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
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_strategies_ASTAStrategy
    com_alphaflow_engine_strategies_ASTAStrategy ==x com_alphaflow_api_dtos_ASTAResponseDTO
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_entities_ASTAResults
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_enums_Timeframe
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_repositories_ASTAResultsRepository
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_repositories_DailyIndicatorRepository
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_strategies_ASTAStrategy --> com_alphaflow_persistence_repositories_WeeklyIndicatorRepository
    com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_engine_strategies_evaluators_ASTAEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_evaluators_ASTAEvaluator --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluator
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_persistence_enums_EvaluatorMessage
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_strategies_evaluators_DailyEmaEvaluator --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluator
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_persistence_enums_EvaluatorMessage
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_strategies_evaluators_DailyRsiEvaluator --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluator
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_persistence_enums_EvaluatorMessage
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_strategies_evaluators_DailyStochasticEvaluator --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluator
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_enums_EvaluatorMessage
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_strategies_evaluators_DailyVolumeEvaluator --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_engine_strategies_evaluators_TradeSignal --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluationContext
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_engine_strategies_evaluators_ASTAEvaluator
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_engine_strategies_evaluators_TradeSignal
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_persistence_enums_EvaluatorMessage
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_strategies_evaluators_WeeklyMacdEvaluator --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_persistence_entities_ASTAResults --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_ASTAResults --> com_alphaflow_persistence_enums_TradeAction
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_DailyPrice --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_Indicator ==x com_alphaflow_engine_indicators_dtos_IndicatorParams
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
    com_alphaflow_persistence_repositories_ASTAResultsRepository --> com_alphaflow_persistence_entities_ASTAResults
    com_alphaflow_persistence_repositories_ASTAResultsRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyPriceRepository ==x com_alphaflow_api_dtos_OhlcvDTO
    com_alphaflow_persistence_repositories_DailyPriceRepository ==x com_alphaflow_api_mappers_OhlcvMapper
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
    linkStyle 153 stroke:#ef4444,stroke-width:3px;
    linkStyle 231 stroke:#ef4444,stroke-width:3px;
    linkStyle 247 stroke:#ef4444,stroke-width:3px;
    linkStyle 248 stroke:#ef4444,stroke-width:3px;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_api_dtos_OhlcvDTO violated;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_engine_indicators_dtos_IndicatorParams violated;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_api_mappers_OhlcvMapper violated;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_engine_strategies_ASTAStrategy violated;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_persistence_entities_Indicator violated;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_api_dtos_ASTAResponseDTO violated;
    classDef violated stroke:#ef4444,stroke-width:2px,fill:#fee2e2,color:#991b1b;
    class com_alphaflow_persistence_repositories_DailyPriceRepository violated;
```

## Component Summary

- **Total Classes**: 72
- **Total Dependencies**: 258
- **API Layer Classes**: 21
- **Engine Layer Classes**: 27
- **Persistence Layer Classes**: 23
