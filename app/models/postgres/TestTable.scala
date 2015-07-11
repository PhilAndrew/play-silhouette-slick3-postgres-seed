package models.postgres

//: ----------------------------------------------------------------------------
//:
//: Dependencies:
//: ?
//: ----------------------------------------------------------------------------

import java.sql.Timestamp
import  com.github.tminglei.slickpg.Range
import com.vividsolutions.jts.geom.Point
import utils.postgres.PlayPostgresDriver.api._

case class Test(id: Option[Long],
                during: Range[Timestamp],
                //location: Point,
                text: String,
                props: Map[String,String],
                tags: List[String])

class TestTable(tag: Tag) extends Table[Test](tag, Some("xxx"), "Test") {
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def during = column[Range[Timestamp]]("during")
  //def location = column[Point]("location")
  def text = column[String]("text", O.SqlType("varchar(4000)"))
  def props = column[Map[String,String]]("props_hstore")
  def tags = column[List[String]]("tags_arr")

  def * = (id.?, during, //location,
  text, props, tags) <> (Test.tupled, Test.unapply)
}

object tests extends TableQuery(new TestTable(_)) {
  // will generate sql like:
  //   select * from test where id = ?
  def byId(ids: Long*) = tests
    .filter(_.id inSetBind ids)
    .map(t => t)
  // will generate sql like:
  //   select * from test where tags && ?
  def byTag(tags: String*) = tests
    .filter(_.tags @& tags.toList.bind)
    .map(t => t)
  // will generate sql like:
  //   select * from test where during && ?
  def byTsRange(tsRange: Range[Timestamp]) = tests
    .filter(_.during @& tsRange.bind)
    .map(t => t)
  // will generate sql like:
  //   select * from test where case(props -> ? as [T]) == ?
  /*def byProperty[T](key: String, value: T) = tests
    .filter(_.props.>>[T](key.bind) === value.bind)
    .map(t => t)*/
  // will generate sql like:
  //   select * from test where ST_DWithin(location, ?, ?)
  /*def byDistance(point: Point, distance: Int) = tests
    .filter(r => r.location.dWithin(point.bind, distance.bind))
    .map(t => t)*/
  // will generate sql like:
  //   select id, text, ts_rank(to_tsvector(text), to_tsquery(?))
  //   from test where to_tsvector(text) @@ to_tsquery(?)
  //   order by ts_rank(to_tsvector(text), to_tsquery(?))
  def search(queryStr: String) = tests
    .filter( t => {tsVector(t.text) @@ tsQuery(queryStr.bind)})
    .map(r => (r.id, r.text, tsRank(tsVector(r.text), tsQuery(queryStr.bind))))
    .sortBy(_._3)
}

