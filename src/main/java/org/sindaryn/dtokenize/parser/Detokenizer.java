package org.sindaryn.dtokenize.parser;


import org.sindaryn.dtokenize.error.TokenStreamTemplateMismatchException;
import org.sindaryn.dtokenize.model.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Detokenizer {

    private Detokenizer(){}

    public Map<String, String> parse(String tokenStream) {//throws TokenStreamTemplateMismatchException {
        Map<String, String> mappedResults = new HashMap<>();
        Stack<Character> squareBrackets = new Stack<>();
        char[] chars = tokenStream.toCharArray();
        StringBuilder currentTokenValue = new StringBuilder();
        for (int lineIndex = 0, tokenIndex = 0; lineIndex < chars.length; lineIndex++) {
            handleAnyBracketsOrQuotes(chars, lineIndex, squareBrackets);
            if(isEndOfToken(chars, lineIndex, currentTokenValue, tokenIndex, squareBrackets)) {
                assignTokenValueIfNonStatic(tokenIndex, currentTokenValue, mappedResults);
                tokenIndex++;
                currentTokenValue = new StringBuilder();
            }
            if(isMeaningfulCharacter(chars, lineIndex, squareBrackets))
                currentTokenValue.append(chars[lineIndex]);
        }
        return mappedResults;
    }

    private boolean isMeaningfulCharacter(char[] chars, int lineIndex, Stack<Character> squareBrackets) {
        return chars[lineIndex] != ' ' || isInBetweenBracketsOrQuotes(squareBrackets);
    }

    private boolean isInBetweenDoubleQuotes = false;
    private void handleAnyBracketsOrQuotes(char[] chars, int lineIndex, Stack<Character> squareBrackets) {
        final char c = chars[lineIndex];
        if(c == '[') squareBrackets.push('[');
        else if(c == '"') isInBetweenDoubleQuotes = !isInBetweenDoubleQuotes;
        else if(c == ']') if(!squareBrackets.empty()) squareBrackets.pop();
    }
    private boolean isInBetweenBracketsOrQuotes(Stack<Character> brackets){
        return !brackets.empty() || isInBetweenDoubleQuotes;
    }



    private boolean isEndOfToken(char[] chars, int lineIndex, StringBuilder currentTokenValue, int tokenIndex, Stack<Character> squareBrackets) {
        final boolean currentTokenValueInProgress = !currentTokenValue.toString().equals("");
        if(lineIndex + 1 == chars.length)
            return true;
        else if(chars[lineIndex] == ' ' && !isInBetweenBracketsOrQuotes(squareBrackets))
            return currentTokenValueInProgress;
        else if(isClosingBracketOrDoubleQuote(chars[lineIndex], squareBrackets))
            return true;
        else return matchesStaticExpression(tokenIndex, currentTokenValue);
    }



    private boolean isClosingBracketOrDoubleQuote(char c, Stack<Character> squareBrackets) {
        return (c == ']' && squareBrackets.empty())
                ||
                String.valueOf(c).equals("\"") && !isInBetweenDoubleQuotes;
    }

    private void assignTokenValueIfNonStatic(int tokenIndex, StringBuilder currentTokenValue, Map<String, String> resultsMap) {
        String value = currentTokenValue.toString();
        final Token expectedToken = expectedTokens.get(tokenIndex);
        if(!expectedToken.getIsStaticExpression())
            expectedToken.setMappedValue(resultsMap, value);
    }

    private boolean matchesStaticExpression(int tokenIndex, StringBuilder value) {
        return expectedTokens.get(tokenIndex).getIsStaticExpression() &&
                expectedTokens.get(tokenIndex).getExpression().equals(value.toString());
    }
    private static Token parseRawToken(String currentTokenTemplate){
        Token token = new Token();
        token.setIsStaticExpression(determineTokenType(currentTokenTemplate));
        currentTokenTemplate = currentTokenTemplate.substring(1);//strip '$' / '^' prefix
        token.setExpression(currentTokenTemplate);
        return token;
    }

    public static Detokenizer of(String template){
        Detokenizer instance = new Detokenizer();
        char[] charArray = template.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if(c == '$' || c == '^')
                instance.expectedTokens.add(parseRawToken(template.substring(i, endOfToken(i, template))));
        }
        return instance;
    }

    private static int endOfToken(int i, String logFormat) {
        Pattern p = Pattern.compile("[\\s+$^]");
        Matcher m = p.matcher(logFormat.substring(i + 1));
        if (m.find())
            return i + m.start() + 1;
        return logFormat.length();
    }
    private List<Token> expectedTokens = new ArrayList<>();
    private static boolean determineTokenType(String s) {
        if(s.startsWith("$")) return false;
        if(s.startsWith("^")) return true;
        throw new IllegalArgumentException("Invalid log-file format: " + s);
    }
}