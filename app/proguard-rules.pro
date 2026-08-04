# Regras ProGuard padrão para o módulo app
# Adicionar regras específicas conforme necessário
-keepattributes Signature
-keepattributes *Annotation*

# Manter modelos de resposta da API para o Gson
-keep class br.com.lit.busca.material.data.remote.** { *; }
