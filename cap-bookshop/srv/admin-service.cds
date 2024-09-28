using { sap.capire.bookshop as my } from '../db/schema';
service AdminService @(requires:'admin', path:'/admin') {
  entity Books as projection on my.Books
  actions {
    action SetStatusReject () returns String;
  };
  entity Authors as projection on my.Authors;
  entity AuthorsByMultKey as projection on my.AuthorsByMultKey;
  entity AuthorsByDateTimeKey as projection on my.AuthorsByDateTimeKey;
  entity AuthorsByMultKeyDateTime as projection on my.AuthorsByMultKeyDateTime;
}