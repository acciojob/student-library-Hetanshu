package com.example.library.studentlibrary.services;

import com.example.library.studentlibrary.models.*;
import com.example.library.studentlibrary.repositories.BookRepository;
import com.example.library.studentlibrary.repositories.CardRepository;
import com.example.library.studentlibrary.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    BookRepository bookRepository5;

    @Autowired
    CardRepository cardRepository5;

    @Autowired
    TransactionRepository transactionRepository5;

    @Value("${books.max_allowed}")
    int max_allowed_books;

    @Value("${books.max_allowed_days}")
    int getMax_allowed_days;

    @Value("${books.fine.per_day}")
    int fine_per_day;

    public String issueBook(int cardId, int bookId) throws Exception {
        Optional<Book> book=bookRepository5.findById(bookId);
        Optional<Card> card=cardRepository5.findById(cardId);
        //check whether bookId and cardId already exist
        //conditions required for successful transaction of issue book:
        //1. book is present and available
        // If it fails: throw new Exception("Book is either unavailable or not present");
        //2. card is present and activated
        // If it fails: throw new Exception("Card is invalid");
        //3. number of books issued against the card is strictly less than max_allowed_books
        // If it fails: throw new Exception("Book limit has reached for this card");
        //If the transaction is successful, save the transaction to the list of transactions and return the id

        //Note that the error message should match exactly in all cases

        if(book.isPresent() && book.get().isAvailable()){
            if(card.isPresent() && card.get().getCardStatus()==CardStatus.ACTIVATED){
                if(card.get().getBooks().size()<max_allowed_books){
                    Transaction transaction=Transaction.builder()
                            .transactionDate(new Date())
                            .transactionStatus(TransactionStatus.SUCCESSFUL)
                            .isIssueOperation(true)
                            .book(book.get()).card(card.get()).build();

                    List<Book> books=card.get().getBooks();
                    books.add(book.get());
                    card.get().setBooks(books);
                    cardRepository5.save(card.get());

                    book.get().setAvailable(false);
                    book.get().setCard(card.get());
                    List<Transaction> transactions=book.get().getTransactions();
                    transactions.add(transaction);
                    book.get().setTransactions(transactions);
                    bookRepository5.save(book.get());

                    transactionRepository5.save(transaction);
                    return transaction.getTransactionId();
                }else{
                    try{
                        throw new Exception("Book limit has reached for this card");
                    }catch (Exception e){
                        System.out.println(e);
                    }
                }
            }else{
                try {
                    throw new Exception("Card is invalid");
                }catch (Exception e){
                    System.out.println(e);
                }
            }
        }else{
            try {
                throw new Exception("Book is either unavailable or not present");
            } catch (Exception e){
                System.out.println(e);
            }
        }

        Transaction transaction=Transaction.builder()
                .transactionDate(new Date()).transactionStatus(TransactionStatus.FAILED)
                .isIssueOperation(true).build();
        transactionRepository5.save(transaction);
       return null; //return transactionId instead
    }

    public Transaction returnBook(int cardId, int bookId) throws Exception{

        List<Transaction> transactions = transactionRepository5.find(cardId, bookId,TransactionStatus.SUCCESSFUL, true);
        Transaction transaction=null;
        try{
            transaction=transactions.get(transactions.size() - 1);
        }catch (Exception e){
            System.out.println(e);
        }

        Date book_issue_date=transaction.getTransactionDate();


        //create instance of the Calendar class and set the date to the given date
        Calendar c = Calendar.getInstance();
        try{
            c.setTime(book_issue_date);
        }catch(Exception e){
            System.out.println(e);
        }

        c.add(Calendar.DATE,getMax_allowed_days);
        Date last_date_for_return=c.getTime();

        Date book_return_date=new Date();

        long time_difference=book_return_date.getTime() - last_date_for_return.getTime();

        long days_difference=(time_difference/(1000*60*60*24));
        int fine=(int)days_difference * fine_per_day;

        Card card=cardRepository5.findById(cardId).get();
        Book book=bookRepository5.findById(bookId).get();

        List<Book> books=card.getBooks();
        books.remove(book);
        card.setBooks(books);
        cardRepository5.save(card);

        book.setAvailable(true);
        List<Transaction> transactionList=book.getTransactions();
        Transaction newTransaction=Transaction.builder()
                .transactionDate(book_return_date).transactionStatus(TransactionStatus.SUCCESSFUL)
                .isIssueOperation(false).fineAmount(fine).book(book).card(card).build();
        transactionList.add(newTransaction);
        book.setTransactions(transactionList);
        bookRepository5.save(book);
        transactionRepository5.save(newTransaction);
        //for the given transaction calculate the fine amount considering the book has been returned exactly when this function is called
        //make the book available for other users
        //make a new transaction for return book which contains the fine amount as well

        return newTransaction; //return the transaction after updating all details
    }
}