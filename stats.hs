module Main where

import Data.List
import System.Environment

io f = interact (unlines . f . lines)

main = io (stats)

stats :: [String] -> [String]
stats s = ["Stats by hour"] ++ showLists [firstAppearances apId, lastAppearances apId, uniqAppearances apId]
--stats s = showLists [uniqAppearances ap]
  where apHour = arrangePts (ptsHour s)
        apId = arrangePts (ptsId s)

firstAppearances :: (Ord b) => [[(a,b)]] -> [(b,Integer)]
firstAppearances = counts . sort . (map snd) . (map head)

lastAppearances :: (Ord b) => [[(a,b)]] -> [(b,Integer)]
lastAppearances = counts . sort . (map snd) . (map last)

uniqAppearances :: (Ord b) => [[(a,b)]] -> [(b,Integer)]
uniqAppearances = counts . sort . (map snd) . oneTime

oneTime :: [[a]] -> [a]
oneTime ([]) = []
oneTime (x:xs) | length x == 1 = (head x):(oneTime xs)
               | otherwise = oneTime xs

cmppt :: (Ord a, Ord b) => (a,b) -> (a,b) -> Ordering
cmppt a b | (fst a) == (fst b) = compare (snd a) (snd b)
cmppt a b = compare (fst a) (fst b)

ltopt :: String -> (Integer,Integer)
ltopt l = (a,b) where
  a:b:_ = map read (words l)

ptsId :: [String] -> [(Integer,Integer)]
ptsId = (sortBy cmppt) . (map swp) . pts
  where swp (a,b) = (b,a)

ptsHour :: [String] -> [(Integer,Integer)]
ptsHour = (sortBy cmppt) . pts

pts :: [String] -> [(Integer,Integer)]
pts = map ltopt

arrangePts :: (Eq a) => [(a,b)] -> [[(a,b)]]
arrangePts ((a,b):[]) = [[(a,b)]]
arrangePts ((a,b):(c,d):pts) | a == c = ((a,b):(head ap)):(tail ap)
                             | otherwise = ((a,b):[]):ap
  where ap = arrangePts ((c,d):pts)

counts :: (Eq a) => [a] -> [(a, Integer)]
counts (a:[]) = (a,1):[]
counts (a:b:bs) | a == b = (a,1+(snd (head c))):(tail c)
		| otherwise = (a,1):c
  where c = counts (b:bs)

showdata :: (Show t) => [t] -> String
showdata (a:[]) = show a
showdata (a:as) = (show a) ++ "\t" ++ (showdata as)

dropIf :: (a -> Bool) -> [a] -> [a]
dropIf f [] = []
dropIf f (a:as) | f a = as
                | otherwise = (a:as)

takeIf :: (a -> Bool) -> [a] -> [a]
takeIf f [] = []
takeIf f (a:as) | f a = [a]
                | otherwise = []

splitIf :: (a -> Bool) -> [a] -> ([a],[a])
splitIf f as = ((takeIf f as),(dropIf f as))

showLists :: (Show a) => [[(Integer,a)]] -> [String]
showLists cols | all ((== 0) . length) cols = [""]
showLists cols = [showRow row] ++ (showLists morecols)
  where splitcols = map (splitIf ((== (minx cols)) . fst)) cols
	row = map fst splitcols
	morecols = map snd splitcols
	
minx (a:[]) = fst (head a)
minx (a:b:cs) | length a == 0 = minx (b:cs)
              | length b == 0 = minx (a:cs)
              | otherwise = min (fst (head a)) (minx (b:cs))

showRow :: (Show a) => [[(Integer,a)]] -> String
showRow pts = (show x) ++ "\t" ++ concat (map showy pts)
  where x = fst (head (head (dropWhile ((== 0) . length) pts)))
        showy pt | length pt == 0 = "\t"
	         | otherwise = show (snd (head pt)) ++ "\t"
