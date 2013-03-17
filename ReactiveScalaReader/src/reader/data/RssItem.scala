package reader.data

import scala.xml.Node
import java.util.Date
import java.net.URL

/**
 * A channel may contain any number of <item>s. An item may represent a
 * "story" -- much like a story in a newspaper or magazine; if so its
 * description is a synopsis of the story, and the link points to the
 * full story. An item may also be complete in itself, if so, the
 * description contains the text (entity-encoded HTML is allowed; see
 * examples), and the link and title may be omitted. All elements of an
 * item are optional, however at least one of title or description must be present.
 *
 * @param title       - title of the item
 * @param link        - URL of the item
 * @param description - the item synopsis
 *
 * optional
 *
 * @param pubDate    - indicates when the item was published
 * @param srcChannel - the RSSChannel that the item came from
 */
case class RSSItem( val title       : String
                  , val link        : Option[URL]
                  , val description : String
                  , val pubDate     : Option[Date]
		              , val srcChannel  : Option[RSSChannel])
		              extends Ordered[RSSItem] {

  def compare(that: RSSItem) = {
    val result = for {
      thisDate <- this.pubDate
      thatDate <- that.pubDate
    } yield { thisDate.compareTo(thatDate) }

    result.getOrElse(0)
  }
}

object RSSItem{
  def changeSource(item: RSSItem,src: Option[RSSChannel]) = item match {
    case RSSItem(title,link,desc,date,_) => RSSItem(title,link,desc,date,src)
  }
}
