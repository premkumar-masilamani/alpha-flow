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
        com_alphaflow_api_dtos_ChartPatternPivotDto["ChartPatternPivotDto (record)"]
        com_alphaflow_api_dtos_OhlcvDto["OhlcvDto (record)"]
        com_alphaflow_api_dtos_ChartPatternDto["ChartPatternDto (record)"]
        com_alphaflow_api_dtos_CandlestickPatternDto["CandlestickPatternDto (record)"]
        com_alphaflow_api_dtos_TickerDto["TickerDto (record)"]
        com_alphaflow_api_dtos_IndicatorSeriesDto["IndicatorSeriesDto (record)"]
        com_alphaflow_api_dtos_SupportResistanceDto["SupportResistanceDto (class)"]
        com_alphaflow_api_dtos_IndicatorConfigDto["IndicatorConfigDto (record)"]
        com_alphaflow_api_dtos_IndicatorPointDto["IndicatorPointDto (record)"]
        com_alphaflow_api_dtos_QuoteDto["QuoteDto (record)"]
        com_alphaflow_api_configs_CorsConfig["CorsConfig (class)"]
        com_alphaflow_api_mappers_IndicatorMapper["IndicatorMapper (class)"]
        com_alphaflow_api_mappers_OhlcvMapper["OhlcvMapper (class)"]
        com_alphaflow_api_mappers_TickerMapper["TickerMapper (class)"]
        com_alphaflow_api_controllers_ChartPatternController["ChartPatternController (class)"]
        com_alphaflow_api_controllers_CandlestickPatternController["CandlestickPatternController (class)"]
        com_alphaflow_api_controllers_TickerController["TickerController (class)"]
        com_alphaflow_api_controllers_PriceController["PriceController (class)"]
        com_alphaflow_api_controllers_IndicatorController["IndicatorController (class)"]
        com_alphaflow_api_controllers_SupportResistanceController["SupportResistanceController (class)"]
        com_alphaflow_api_controllers_generic_GlobalExceptionHandler["GlobalExceptionHandler (class)"]
        com_alphaflow_api_controllers_generic_ApiController["ApiController (class)"]
        com_alphaflow_api_services_DailyPriceService["DailyPriceService (class)"]
        com_alphaflow_api_services_TickerService["TickerService (class)"]
        com_alphaflow_api_services_IntradayPriceService["IntradayPriceService (class)"]
        com_alphaflow_api_services_QuoteService["QuoteService (class)"]
        com_alphaflow_api_services_ChartPatternService["ChartPatternService (class)"]
        com_alphaflow_api_services_WeeklyPriceService["WeeklyPriceService (class)"]
        com_alphaflow_api_services_IndicatorService["IndicatorService (class)"]
        com_alphaflow_api_services_CandlestickPatternService["CandlestickPatternService (class)"]
        com_alphaflow_api_services_SupportResistanceService["SupportResistanceService (class)"]
    end

    subgraph engine ["Engine (Logic)"]
        com_alphaflow_engine_indicators_SmaIndicator["SmaIndicator (class)"]
        com_alphaflow_engine_indicators_Indicator["Indicator (interface)"]
        com_alphaflow_engine_indicators_RsiIndicator["RsiIndicator (class)"]
        com_alphaflow_engine_indicators_MacdIndicator["MacdIndicator (class)"]
        com_alphaflow_engine_indicators_StochasticIndicator["StochasticIndicator (class)"]
        com_alphaflow_engine_indicators_EmaIndicator["EmaIndicator (class)"]
        com_alphaflow_engine_indicators_BollingerBandsIndicator["BollingerBandsIndicator (class)"]
        com_alphaflow_engine_indicators_utils_EmaAccumulator["EmaAccumulator (class)"]
        com_alphaflow_engine_indicators_utils_IndicatorMath["IndicatorMath (class)"]
        com_alphaflow_engine_indicators_utils_IndicatorRegistry["IndicatorRegistry (class)"]
        com_alphaflow_engine_indicators_dtos_IndicatorParams["IndicatorParams (class)"]
        com_alphaflow_engine_indicators_dtos_PriceBar["PriceBar (record)"]
        com_alphaflow_engine_downloaders_YahooFinanceDownloader["YahooFinanceDownloader (class)"]
        com_alphaflow_engine_downloaders_AngelOneDownloader["AngelOneDownloader (class)"]
        com_alphaflow_engine_downloaders_YahooResponseParser["YahooResponseParser (class)"]
        com_alphaflow_engine_downloaders_dtos_YahooResult["YahooResult (record)"]
        com_alphaflow_engine_downloaders_dtos_YahooQuote["YahooQuote (record)"]
        com_alphaflow_engine_downloaders_dtos_YahooResponse["YahooResponse (record)"]
        com_alphaflow_engine_downloaders_dtos_YahooChart["YahooChart (record)"]
        com_alphaflow_engine_downloaders_dtos_YahooIndicators["YahooIndicators (record)"]
        com_alphaflow_engine_downloaders_angelone_AngelOneNetworkHelper["AngelOneNetworkHelper (class)"]
        com_alphaflow_engine_downloaders_angelone_AngelOneClient["AngelOneClient (class)"]
        com_alphaflow_engine_downloaders_angelone_TotpGenerator["TotpGenerator (class)"]
        com_alphaflow_engine_downloaders_angelone_AngelOneAuthManager["AngelOneAuthManager (class)"]
        com_alphaflow_engine_downloaders_angelone_dtos_AngelOneCandle["AngelOneCandle (record)"]
        com_alphaflow_engine_downloaders_angelone_dtos_AngelOneQuote["AngelOneQuote (record)"]
        com_alphaflow_engine_configs_JacksonConfig["JacksonConfig (class)"]
        com_alphaflow_engine_configs_YahooFinanceConfig["YahooFinanceConfig (class)"]
        com_alphaflow_engine_configs_IndicatorConfig["IndicatorConfig (class)"]
        com_alphaflow_engine_configs_AngelOneConfig["AngelOneConfig (class)"]
        com_alphaflow_engine_calculators_CandlestickPatternCalculator["CandlestickPatternCalculator (class)"]
        com_alphaflow_engine_calculators_WeeklyPriceCalculator["WeeklyPriceCalculator (class)"]
        com_alphaflow_engine_calculators_ChartPatternCalculator["ChartPatternCalculator (class)"]
        com_alphaflow_engine_calculators_SupportResistanceCalculator["SupportResistanceCalculator (class)"]
        com_alphaflow_engine_calculators_IndicatorCalculator["IndicatorCalculator (class)"]
        com_alphaflow_engine_calculators_enums_LevelType["LevelType (enum)"]
        com_alphaflow_engine_calculators_dtos_ExtremaPoint["ExtremaPoint (record)"]
        com_alphaflow_engine_calculators_dtos_PatternMatch["PatternMatch (record)"]
        com_alphaflow_engine_calculators_dtos_ChartPatternMatch["ChartPatternMatch (record)"]
        com_alphaflow_engine_calculators_dtos_Bucket["Bucket (class)"]
        com_alphaflow_engine_schedulers_AngelOneScheduler["AngelOneScheduler (class)"]
        com_alphaflow_engine_schedulers_CoreScheduler["CoreScheduler (class)"]
    end

    subgraph persistence ["Persistence (Database)"]
        com_alphaflow_persistence_enums_Country["Country (enum)"]
        com_alphaflow_persistence_enums_ChartPatternStatus["ChartPatternStatus (enum)"]
        com_alphaflow_persistence_enums_PatternSentiment["PatternSentiment (enum)"]
        com_alphaflow_persistence_enums_DataProvider["DataProvider (enum)"]
        com_alphaflow_persistence_enums_TickerType["TickerType (enum)"]
        com_alphaflow_persistence_enums_IndicatorOutputKey["IndicatorOutputKey (enum)"]
        com_alphaflow_persistence_enums_IndicatorParamKey["IndicatorParamKey (enum)"]
        com_alphaflow_persistence_enums_IndicatorType["IndicatorType (enum)"]
        com_alphaflow_persistence_enums_PriceSource["PriceSource (enum)"]
        com_alphaflow_persistence_enums_ChartPatternType["ChartPatternType (enum)"]
        com_alphaflow_persistence_enums_CandlestickPattern["CandlestickPattern (enum)"]
        com_alphaflow_persistence_repositories_TickerRepository["TickerRepository (interface)"]
        com_alphaflow_persistence_repositories_IntradayPriceRepository["IntradayPriceRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklySupportResistanceRepository["WeeklySupportResistanceRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyPriceRepository["WeeklyPriceRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyCandlestickPatternRepository["DailyCandlestickPatternRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyChartPatternRepository["WeeklyChartPatternRepository (interface)"]
        com_alphaflow_persistence_repositories_DailySupportResistanceRepository["DailySupportResistanceRepository (interface)"]
        com_alphaflow_persistence_repositories_IndicatorDefinitionRepository["IndicatorDefinitionRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyCandlestickPatternRepository["WeeklyCandlestickPatternRepository (interface)"]
        com_alphaflow_persistence_repositories_WeeklyIndicatorRepository["WeeklyIndicatorRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyIndicatorRepository["DailyIndicatorRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyPriceRepository["DailyPriceRepository (interface)"]
        com_alphaflow_persistence_repositories_DailyChartPatternRepository["DailyChartPatternRepository (interface)"]
        com_alphaflow_persistence_exceptions_ResourceNotFoundException["ResourceNotFoundException (class)"]
        com_alphaflow_persistence_entities_DailyIndicator["DailyIndicator (class)"]
        com_alphaflow_persistence_entities_ChartPatternPivot["ChartPatternPivot (class)"]
        com_alphaflow_persistence_entities_IntradayPrice["IntradayPrice (class)"]
        com_alphaflow_persistence_entities_WeeklyChartPattern["WeeklyChartPattern (class)"]
        com_alphaflow_persistence_entities_WeeklyCandlestickPattern["WeeklyCandlestickPattern (class)"]
        com_alphaflow_persistence_entities_WeeklyIndicator["WeeklyIndicator (class)"]
        com_alphaflow_persistence_entities_IndicatorDefinition["IndicatorDefinition (class)"]
        com_alphaflow_persistence_entities_DailySupportResistance["DailySupportResistance (class)"]
        com_alphaflow_persistence_entities_DailyPrice["DailyPrice (class)"]
        com_alphaflow_persistence_entities_Indicator["Indicator (class)"]
        com_alphaflow_persistence_entities_WeeklyPrice["WeeklyPrice (class)"]
        com_alphaflow_persistence_entities_WeeklySupportResistance["WeeklySupportResistance (class)"]
        com_alphaflow_persistence_entities_DailyChartPattern["DailyChartPattern (class)"]
        com_alphaflow_persistence_entities_DailyCandlestickPattern["DailyCandlestickPattern (class)"]
        com_alphaflow_persistence_entities_Ticker["Ticker (class)"]
    end

    com_alphaflow_AlphaFlowApp["AlphaFlowApp (Root)"]
    com_alphaflow_common_constants_MarketConstants["MarketConstants (Root)"]
    com_alphaflow_common_enums_Timeframe["Timeframe (Root)"]

    %% Edges
    com_alphaflow_api_config_TimeframeConverter --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_CandlestickPatternController --> com_alphaflow_api_dtos_CandlestickPatternDto
    com_alphaflow_api_controllers_CandlestickPatternController --> com_alphaflow_api_services_CandlestickPatternService
    com_alphaflow_api_controllers_CandlestickPatternController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_CandlestickPatternController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_CandlestickPatternController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_ChartPatternController --> com_alphaflow_api_dtos_ChartPatternDto
    com_alphaflow_api_controllers_ChartPatternController --> com_alphaflow_api_services_ChartPatternService
    com_alphaflow_api_controllers_ChartPatternController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_ChartPatternController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_ChartPatternController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_ChartPatternController --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_dtos_IndicatorConfigDto
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_dtos_IndicatorSeriesDto
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_services_IndicatorService
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_IndicatorController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_dtos_OhlcvDto
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_dtos_QuoteDto
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_DailyPriceService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_IntradayPriceService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_QuoteService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_api_services_WeeklyPriceService
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_PriceController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_SupportResistanceController --> com_alphaflow_api_dtos_SupportResistanceDto
    com_alphaflow_api_controllers_SupportResistanceController --> com_alphaflow_api_services_SupportResistanceService
    com_alphaflow_api_controllers_SupportResistanceController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_SupportResistanceController --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_controllers_SupportResistanceController --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_controllers_TickerController --> com_alphaflow_api_dtos_TickerDto
    com_alphaflow_api_controllers_TickerController --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_controllers_generic_GlobalExceptionHandler --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_dtos_CandlestickPatternDto --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_api_dtos_ChartPatternDto --> com_alphaflow_api_dtos_ChartPatternPivotDto
    com_alphaflow_api_dtos_ChartPatternDto --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_api_dtos_ChartPatternDto --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_api_dtos_ChartPatternDto --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_api_dtos_IndicatorPointDto --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_api_dtos_IndicatorSeriesDto --> com_alphaflow_api_dtos_IndicatorPointDto
    com_alphaflow_api_dtos_TickerDto --> com_alphaflow_persistence_enums_Country
    com_alphaflow_api_dtos_TickerDto --> com_alphaflow_persistence_enums_TickerType
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorConfigDto
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorPointDto
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_api_dtos_IndicatorSeriesDto
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_api_mappers_IndicatorMapper --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_api_dtos_OhlcvDto
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_common_constants_MarketConstants
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_IntradayPrice
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_api_mappers_OhlcvMapper --> com_alphaflow_persistence_enums_Country
    com_alphaflow_api_mappers_TickerMapper --> com_alphaflow_api_dtos_TickerDto
    com_alphaflow_api_mappers_TickerMapper --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_api_dtos_CandlestickPatternDto
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_enums_CandlestickPattern
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_repositories_DailyCandlestickPatternRepository
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_repositories_WeeklyCandlestickPatternRepository
    com_alphaflow_api_services_CandlestickPatternService --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_api_dtos_ChartPatternDto
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_api_dtos_ChartPatternPivotDto
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_entities_ChartPatternPivot
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_entities_DailyChartPattern
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_entities_WeeklyChartPattern
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_repositories_DailyChartPatternRepository
    com_alphaflow_api_services_ChartPatternService --> com_alphaflow_persistence_repositories_WeeklyChartPatternRepository
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_api_dtos_OhlcvDto
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_api_mappers_OhlcvMapper
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_DailyPriceService --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_dtos_IndicatorConfigDto
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_dtos_IndicatorSeriesDto
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_api_mappers_IndicatorMapper
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_DailyIndicatorRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_WeeklyIndicatorRepository
    com_alphaflow_api_services_IndicatorService --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_api_services_IntradayPriceService --> com_alphaflow_api_dtos_OhlcvDto
    com_alphaflow_api_services_IntradayPriceService --> com_alphaflow_api_mappers_OhlcvMapper
    com_alphaflow_api_services_IntradayPriceService --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_services_IntradayPriceService --> com_alphaflow_persistence_entities_IntradayPrice
    com_alphaflow_api_services_IntradayPriceService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_IntradayPriceService --> com_alphaflow_persistence_repositories_IntradayPriceRepository
    com_alphaflow_api_services_QuoteService --> com_alphaflow_api_dtos_QuoteDto
    com_alphaflow_api_services_QuoteService --> com_alphaflow_common_constants_MarketConstants
    com_alphaflow_api_services_QuoteService --> com_alphaflow_engine_downloaders_angelone_AngelOneClient
    com_alphaflow_api_services_QuoteService --> com_alphaflow_engine_downloaders_angelone_dtos_AngelOneQuote
    com_alphaflow_api_services_QuoteService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_QuoteService --> com_alphaflow_persistence_enums_DataProvider
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_api_dtos_SupportResistanceDto
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_persistence_entities_DailySupportResistance
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_persistence_entities_WeeklySupportResistance
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_persistence_repositories_DailySupportResistanceRepository
    com_alphaflow_api_services_SupportResistanceService --> com_alphaflow_persistence_repositories_WeeklySupportResistanceRepository
    com_alphaflow_api_services_TickerService --> com_alphaflow_api_dtos_TickerDto
    com_alphaflow_api_services_TickerService --> com_alphaflow_api_mappers_TickerMapper
    com_alphaflow_api_services_TickerService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_TickerService --> com_alphaflow_persistence_exceptions_ResourceNotFoundException
    com_alphaflow_api_services_TickerService --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_api_dtos_OhlcvDto
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_api_mappers_OhlcvMapper
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_api_services_TickerService
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_api_services_WeeklyPriceService --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_engine_calculators_dtos_PatternMatch
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_entities_DailyCandlestickPattern
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_entities_WeeklyCandlestickPattern
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_enums_CandlestickPattern
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_repositories_DailyCandlestickPatternRepository
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_repositories_WeeklyCandlestickPatternRepository
    com_alphaflow_engine_calculators_CandlestickPatternCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_engine_calculators_dtos_ChartPatternMatch
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_engine_calculators_dtos_ExtremaPoint
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_entities_ChartPatternPivot
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_entities_DailyChartPattern
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_entities_WeeklyChartPattern
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_repositories_DailyChartPatternRepository
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_repositories_WeeklyChartPatternRepository
    com_alphaflow_engine_calculators_ChartPatternCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_configs_IndicatorConfig
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_engine_indicators_utils_IndicatorRegistry
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_DailyIndicatorRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_WeeklyIndicatorRepository
    com_alphaflow_engine_calculators_IndicatorCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_engine_calculators_dtos_Bucket
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_engine_calculators_enums_LevelType
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_entities_DailySupportResistance
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_entities_WeeklySupportResistance
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_repositories_DailySupportResistanceRepository
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_SupportResistanceCalculator --> com_alphaflow_persistence_repositories_WeeklySupportResistanceRepository
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_calculators_WeeklyPriceCalculator --> com_alphaflow_persistence_repositories_WeeklyPriceRepository
    com_alphaflow_engine_calculators_dtos_Bucket --> com_alphaflow_engine_calculators_enums_LevelType
    com_alphaflow_engine_calculators_dtos_ChartPatternMatch --> com_alphaflow_persistence_entities_ChartPatternPivot
    com_alphaflow_engine_calculators_dtos_ChartPatternMatch --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_engine_calculators_dtos_ChartPatternMatch --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_engine_calculators_dtos_ChartPatternMatch --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_engine_calculators_dtos_PatternMatch --> com_alphaflow_persistence_enums_CandlestickPattern
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_engine_configs_IndicatorConfig --> com_alphaflow_persistence_repositories_IndicatorDefinitionRepository
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_common_constants_MarketConstants
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_engine_configs_AngelOneConfig
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_engine_downloaders_angelone_AngelOneClient
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_engine_downloaders_angelone_dtos_AngelOneCandle
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_persistence_entities_IntradayPrice
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_persistence_enums_DataProvider
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_persistence_repositories_IntradayPriceRepository
    com_alphaflow_engine_downloaders_AngelOneDownloader --> com_alphaflow_persistence_repositories_TickerRepository
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_engine_configs_YahooFinanceConfig
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_engine_downloaders_YahooResponseParser
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_downloaders_YahooFinanceDownloader --> com_alphaflow_persistence_repositories_DailyPriceRepository
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_engine_downloaders_dtos_YahooQuote
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_engine_downloaders_dtos_YahooResponse
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_engine_downloaders_dtos_YahooResult
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_engine_downloaders_YahooResponseParser --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_engine_downloaders_angelone_AngelOneAuthManager --> com_alphaflow_engine_configs_AngelOneConfig
    com_alphaflow_engine_downloaders_angelone_AngelOneAuthManager --> com_alphaflow_engine_downloaders_angelone_AngelOneNetworkHelper
    com_alphaflow_engine_downloaders_angelone_AngelOneAuthManager --> com_alphaflow_engine_downloaders_angelone_TotpGenerator
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_common_constants_MarketConstants
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_common_enums_Timeframe
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_engine_configs_AngelOneConfig
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_engine_downloaders_angelone_AngelOneAuthManager
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_engine_downloaders_angelone_AngelOneNetworkHelper
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_engine_downloaders_angelone_dtos_AngelOneCandle
    com_alphaflow_engine_downloaders_angelone_AngelOneClient --> com_alphaflow_engine_downloaders_angelone_dtos_AngelOneQuote
    com_alphaflow_engine_downloaders_dtos_YahooChart --> com_alphaflow_engine_downloaders_dtos_YahooResult
    com_alphaflow_engine_downloaders_dtos_YahooIndicators --> com_alphaflow_engine_downloaders_dtos_YahooQuote
    com_alphaflow_engine_downloaders_dtos_YahooResponse --> com_alphaflow_engine_downloaders_dtos_YahooChart
    com_alphaflow_engine_downloaders_dtos_YahooResult --> com_alphaflow_engine_downloaders_dtos_YahooIndicators
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_BollingerBandsIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_engine_indicators_utils_EmaAccumulator
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_EmaIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_Indicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_engine_indicators_utils_EmaAccumulator
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_MacdIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_RsiIndicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_engine_indicators_dtos_IndicatorParams
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_engine_indicators_dtos_PriceBar
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_persistence_enums_IndicatorOutputKey
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_persistence_enums_IndicatorParamKey
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_indicators_SmaIndicator --> com_alphaflow_persistence_enums_PriceSource
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
    com_alphaflow_engine_indicators_utils_EmaAccumulator --> com_alphaflow_engine_indicators_utils_IndicatorMath
    com_alphaflow_engine_indicators_utils_IndicatorRegistry --> com_alphaflow_engine_indicators_Indicator
    com_alphaflow_engine_indicators_utils_IndicatorRegistry --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_engine_schedulers_AngelOneScheduler --> com_alphaflow_common_constants_MarketConstants
    com_alphaflow_engine_schedulers_AngelOneScheduler --> com_alphaflow_engine_downloaders_AngelOneDownloader
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_CandlestickPatternCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_ChartPatternCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_IndicatorCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_SupportResistanceCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_calculators_WeeklyPriceCalculator
    com_alphaflow_engine_schedulers_CoreScheduler --> com_alphaflow_engine_downloaders_YahooFinanceDownloader
    com_alphaflow_persistence_entities_DailyCandlestickPattern --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_DailyCandlestickPattern --> com_alphaflow_persistence_enums_CandlestickPattern
    com_alphaflow_persistence_entities_DailyCandlestickPattern --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_persistence_entities_DailyChartPattern --> com_alphaflow_persistence_entities_ChartPatternPivot
    com_alphaflow_persistence_entities_DailyChartPattern --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_DailyChartPattern --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_persistence_entities_DailyChartPattern --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_persistence_entities_DailyChartPattern --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_DailyIndicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_DailyPrice --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_DailySupportResistance --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_persistence_entities_Indicator --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_persistence_entities_IndicatorDefinition --> com_alphaflow_persistence_enums_IndicatorType
    com_alphaflow_persistence_entities_IndicatorDefinition --> com_alphaflow_persistence_enums_PriceSource
    com_alphaflow_persistence_entities_IntradayPrice --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_Ticker --> com_alphaflow_persistence_enums_Country
    com_alphaflow_persistence_entities_Ticker --> com_alphaflow_persistence_enums_DataProvider
    com_alphaflow_persistence_entities_Ticker --> com_alphaflow_persistence_enums_TickerType
    com_alphaflow_persistence_entities_WeeklyCandlestickPattern --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_WeeklyCandlestickPattern --> com_alphaflow_persistence_enums_CandlestickPattern
    com_alphaflow_persistence_entities_WeeklyCandlestickPattern --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_persistence_entities_WeeklyChartPattern --> com_alphaflow_persistence_entities_ChartPatternPivot
    com_alphaflow_persistence_entities_WeeklyChartPattern --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_WeeklyChartPattern --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_persistence_entities_WeeklyChartPattern --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_persistence_entities_WeeklyChartPattern --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_persistence_entities_WeeklyIndicator --> com_alphaflow_persistence_entities_Indicator
    com_alphaflow_persistence_entities_WeeklyIndicator --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_entities_WeeklyIndicator --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_WeeklyPrice --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_entities_WeeklySupportResistance --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_enums_CandlestickPattern --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_persistence_enums_ChartPatternType --> com_alphaflow_persistence_enums_PatternSentiment
    com_alphaflow_persistence_repositories_DailyCandlestickPatternRepository --> com_alphaflow_persistence_entities_DailyCandlestickPattern
    com_alphaflow_persistence_repositories_DailyCandlestickPatternRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyChartPatternRepository --> com_alphaflow_persistence_entities_DailyChartPattern
    com_alphaflow_persistence_repositories_DailyChartPatternRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyChartPatternRepository --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_persistence_repositories_DailyChartPatternRepository --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_DailyIndicator
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_DailyIndicatorRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyPriceRepository --> com_alphaflow_persistence_entities_DailyPrice
    com_alphaflow_persistence_repositories_DailyPriceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_DailyPriceRepository --> com_alphaflow_persistence_enums_DataProvider
    com_alphaflow_persistence_repositories_DailySupportResistanceRepository --> com_alphaflow_persistence_entities_DailySupportResistance
    com_alphaflow_persistence_repositories_DailySupportResistanceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_IndicatorDefinitionRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_IntradayPriceRepository --> com_alphaflow_persistence_entities_IntradayPrice
    com_alphaflow_persistence_repositories_IntradayPriceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_TickerRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_TickerRepository --> com_alphaflow_persistence_enums_DataProvider
    com_alphaflow_persistence_repositories_WeeklyCandlestickPatternRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyCandlestickPatternRepository --> com_alphaflow_persistence_entities_WeeklyCandlestickPattern
    com_alphaflow_persistence_repositories_WeeklyChartPatternRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyChartPatternRepository --> com_alphaflow_persistence_entities_WeeklyChartPattern
    com_alphaflow_persistence_repositories_WeeklyChartPatternRepository --> com_alphaflow_persistence_enums_ChartPatternStatus
    com_alphaflow_persistence_repositories_WeeklyChartPatternRepository --> com_alphaflow_persistence_enums_ChartPatternType
    com_alphaflow_persistence_repositories_WeeklyIndicatorRepository --> com_alphaflow_persistence_entities_IndicatorDefinition
    com_alphaflow_persistence_repositories_WeeklyIndicatorRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyIndicatorRepository --> com_alphaflow_persistence_entities_WeeklyIndicator
    com_alphaflow_persistence_repositories_WeeklyPriceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklyPriceRepository --> com_alphaflow_persistence_entities_WeeklyPrice
    com_alphaflow_persistence_repositories_WeeklySupportResistanceRepository --> com_alphaflow_persistence_entities_Ticker
    com_alphaflow_persistence_repositories_WeeklySupportResistanceRepository --> com_alphaflow_persistence_entities_WeeklySupportResistance

    %% Styles for Violations
```

## Component Summary

- **Total Classes**: 117
- **Total Dependencies**: 376
- **API Layer Classes**: 32
- **Engine Layer Classes**: 42
- **Persistence Layer Classes**: 40
