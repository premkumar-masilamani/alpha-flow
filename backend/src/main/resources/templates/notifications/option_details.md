*RECOMMENDED OPTION*
*Action:* ${action!}
*Contract:* ${contract!}
*Strike Price:* ${strikePrice!}
*Expiry:* ${expiry!}
*Option Premium \(LTP\):* ₹ ${lastPrice!}
*Intrinsic Value:* ₹ ${intrinsicValue!}
*Time Value \(TV\):* ₹ ${timeValue!}
<#if delta?has_content || iv?has_content || theta?has_content>
*Greeks:* Delta: ${delta!} \| IV: ${iv!}% \| Theta: ${theta!}
</#if>
