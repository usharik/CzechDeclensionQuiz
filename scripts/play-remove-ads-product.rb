#!/usr/bin/env ruby
# Creates or updates the one-time Play product "remove_ads" and activates it.
#
# Usage:
#   scripts/play-remove-ads-product.rb            # show the product as Play has it
#   scripts/play-remove-ads-product.rb 2.99       # upsert with a USD base price, converted to every region
#
# Uses the Play service-account key at pc-api-key.json (or PLAY_STORE_JSON_KEY_PATH). Fastlane does not
# cover the monetization.onetimeproducts API, hence this script.
require "googleauth"
require "json"
require "net/http"

PACKAGE = "com.usharik.app"
PRODUCT_ID = "remove_ads" # keep in sync with REMOVE_ADS_PRODUCT_ID in the app
PURCHASE_OPTION_ID = "lifetime"
API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/#{PACKAGE}"

LISTINGS = {
  "en-US" => ["Remove ads", "Removes all ads from the app forever. One-time purchase."],
  "ru-RU" => ["Отключить рекламу", "Навсегда убирает всю рекламу из приложения. Разовая покупка."],
  "uk" => ["Вимкнути рекламу", "Назавжди прибирає всю рекламу із застосунку. Одноразова покупка."],
  "cs-CZ" => ["Odstranit reklamy", "Natrvalo odstraní z aplikace všechny reklamy. Jednorázový nákup."],
  "de-DE" => ["Werbung entfernen", "Entfernt dauerhaft alle Werbung aus der App. Einmaliger Kauf."],
  "vi" => ["Gỡ quảng cáo", "Gỡ vĩnh viễn mọi quảng cáo khỏi ứng dụng. Mua một lần."],
}.freeze

def token
  key = ENV["PLAY_STORE_JSON_KEY_PATH"] || File.expand_path("../pc-api-key.json", __dir__)
  Google::Auth::ServiceAccountCredentials.make_creds(
    json_key_io: File.open(key), scope: "https://www.googleapis.com/auth/androidpublisher",
  ).fetch_access_token!["access_token"]
end

def call(method, url, body = nil)
  uri = URI(url)
  request = Net::HTTP.const_get(method.capitalize).new(uri)
  request["Authorization"] = "Bearer #{token}"
  request["Content-Type"] = "application/json"
  request.body = JSON.generate(body) if body
  response = Net::HTTP.start(uri.host, uri.port, use_ssl: true) { |http| http.request(request) }
  abort "#{method} #{uri.path} -> #{response.code}\n#{response.body}" unless response.is_a?(Net::HTTPSuccess)
  response.body.to_s.empty? ? {} : JSON.parse(response.body)
end

def money(currency, amount)
  units, fraction = format("%.2f", amount).split(".")
  { currencyCode: currency, units: units, nanos: fraction.to_i * 10_000_000 }
end

if ARGV.empty?
  puts JSON.pretty_generate(call(:get, "#{API}/oneTimeProducts/#{PRODUCT_ID}"))
  exit
end

usd = Float(ARGV[0])
converted = call(:post, "#{API}/pricing:convertRegionPrices", { price: money("USD", usd) })
regional = converted.fetch("convertedRegionPrices").values.map do |region|
  { regionCode: region["regionCode"], price: region["price"], availability: "AVAILABLE" }
end
other = converted["convertedOtherRegionsPrice"] || {}

product = {
  packageName: PACKAGE,
  productId: PRODUCT_ID,
  listings: LISTINGS.map { |language, (title, description)| { languageCode: language, title: title, description: description } },
  purchaseOptions: [{
    purchaseOptionId: PURCHASE_OPTION_ID,
    buyOption: { legacyCompatible: true, multiQuantityEnabled: false },
    regionalPricingAndAvailabilityConfigs: regional,
    newRegionsConfig: {
      availability: "AVAILABLE",
      usdPrice: other["usdPrice"] || money("USD", usd),
      eurPrice: other["eurPrice"] || money("EUR", usd),
    },
  }],
}
query = URI.encode_www_form(
  allowMissing: true,
  updateMask: "listings,purchaseOptions",
  "regionsVersion.version" => converted.dig("regionVersion", "version"),
  latencyTolerance: "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_SENSITIVE",
)
call(:patch, "#{API}/onetimeproducts/#{PRODUCT_ID}?#{query}", product)
puts "Upserted #{PRODUCT_ID}: #{regional.size} regions, base USD #{format('%.2f', usd)}"

call(:post, "#{API}/oneTimeProducts/#{PRODUCT_ID}/purchaseOptions:batchUpdateStates", {
  requests: [{
    activatePurchaseOptionRequest: {
      packageName: PACKAGE,
      productId: PRODUCT_ID,
      purchaseOptionId: PURCHASE_OPTION_ID,
      latencyTolerance: "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_SENSITIVE",
    },
  }],
})
puts "Activated purchase option #{PURCHASE_OPTION_ID}"
