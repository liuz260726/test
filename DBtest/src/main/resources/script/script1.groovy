import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlParser
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

Message processData(Message message) {

    def xmlText = message.getBody(String)
    def root = new XmlParser().parseText(xmlText)

    // 階層や名前空間差異に強く、全SupplyDemandItemを拾う
    def items = root.'**'.findAll { it.name() == 'SupplyDemandItem' }

    // まず「必要な項目だけを正規化したMap」に落とす（trim統一）
    def normalized = items.collect { item ->
        [
            Material: (item.Material?.text() ?: "").trim(),
            StorageLocation: (item.StorageLocation?.text() ?: "").trim(),
            MRPElement: (item.MRPElement?.text() ?: "").trim(),
            MRPElementCategory: (item.MRPElementCategory?.text() ?: "").trim(),
            MRPElementItem: (item.MRPElementItem?.text() ?: "").trim(),
            MRPElementAvailyOrRqmtDate: (item.MRPElementAvailyOrRqmtDate?.text() ?: "").trim(),
            MRPElementOpenQuantity: (item.MRPElementOpenQuantity?.text() ?: "").trim(),
            MRPElementBusinessPartner: (item.MRPElementBusinessPartner?.text() ?: "").trim()
        ]
    }
    // StorageLocation 空は除外
    .findAll { it.StorageLocation }

    // groupByで Material_StorageLocation 単位にまとめる
    def grouped = normalized.groupBy { "${it.Material}_${it.StorageLocation}" }

    // XML生成（MarkupBuilder）
    def sw = new StringWriter()
    def mb = new MarkupBuilder(sw)
    mb.Root {
        grouped.each { idKey, rows ->
            Entry {
                ID(idKey)
                ContentsList {
                    rows.each { r ->
                        Contents {
                            Material(r.Material)
                            StorageLocation(r.StorageLocation)
                            MRPElement(r.MRPElement)
                            MRPElementCategory(r.MRPElementCategory)
                            MRPElementItem(r.MRPElementItem)
                            MRPElementAvailyOrRqmtDate(r.MRPElementAvailyOrRqmtDate)
                            MRPElementOpenQuantity(r.MRPElementOpenQuantity)
                            MRPElementBusinessPartner(r.MRPElementBusinessPartner)

                            // 追加フィールド（空でOK）
                            PurchaseOrderType("")
                            NYUSHUKKA_DENPYO_KUBUN("")
                            DeliveryDocument("")
                            DeliveryDocumentType("")
                            ReferenceSDDocument("")
                            ReferenceSDDocumentCategory("")
                            ReferenceSDDocumentItem("")
                            PartnerFunction("")
                            Customer("")
                            StorageLocationName("")
                            BusinessPartnerFullName("")
                        }
                    }
                }
            }
        }
    }

    message.setBody(XmlUtil.serialize(sw.toString()))
    return message
}
