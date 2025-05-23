using { Currency, managed, sap } from '@sap/cds/common';
namespace sap.capire.bookshop;

entity Books : managed {
  key ID   : Integer;
  title    : localized String(111)  @mandatory;
  descr    : localized String(1111);
  author   : Association to Authors @mandatory;
  genre    : Association to Genres;
  stock    : Integer;
  price    : Decimal;
  currency : Currency;
  image    : LargeBinary @Core.MediaType: 'image/png';
}

entity Authors : managed {
  key ID       : Integer;
  name         : String(111) @mandatory;
  dateOfBirth  : Date;
  dateOfDeath  : Date;
  placeOfBirth : String;
  placeOfDeath : String;
  books        : Association to many Books on books.author = $self;
}

entity AuthorsByMultKey : managed {
  key ID       : Integer;
  key name     : String(111) @mandatory;
  dateOfBirth  : Date;
  dateOfDeath  : Date;
  placeOfBirth : String;
  placeOfDeath : String;
}

entity AuthorsByDateTimeKey : managed {
  key dateOfBirth  : DateTime;
  name         : String(111) @mandatory;
  dateOfDeath  : Date;
  placeOfBirth : String;
  placeOfDeath : String;
}
entity AuthorsByMultKeyDateTime : managed {
  key ID       : Integer;
  key dateOfBirth  : DateTime;
  name         : String(111);
  dateOfDeath  : Date;
  placeOfBirth : String;
  placeOfDeath : String;
}

/** Hierarchically organized Code List for Genres */
entity Genres : sap.common.CodeList {
  key ID   : Integer;
  parent   : Association to Genres;
  children : Composition of many Genres on children.parent = $self;
}
